import warnings
warnings.filterwarnings("ignore", category=FutureWarning)  # silencia warning de pynvml
from codecarbon import EmissionsTracker
import subprocess
import sys
import os
import threading
import time
import json

import platform
try:
    import psutil
    PSUTIL_DISPONIBLE = True
except ImportError:
    PSUTIL_DISPONIBLE = False

ES_WINDOWS = platform.system() == "Windows"
ES_LINUX   = platform.system() == "Linux"


POWERLOG_PATH = os.getenv('POWERLOG_PATH',
    r"C:\Program Files\Intel\Power Gadget 3.6\PowerLog3.0.exe")
CSV_POWERLOG      = "intel_power_gadget_log.csv"
JSON_TEMPERATURAS = "temps_monitor.json"

# Argumentos de entrada
num_runs     = int(sys.argv[1])   if len(sys.argv) > 1 else 1
seed         = int(sys.argv[2])   if len(sys.argv) > 2 else 1
temp_max     = float(sys.argv[3]) if len(sys.argv) > 3 else -1.0
potencia_max = float(sys.argv[4]) if len(sys.argv) > 4 else 0.0   # 0 = sin limite
tiempo_max   = float(sys.argv[5]) if len(sys.argv) > 5 else 0.0   # 0 = sin limite
problema     = sys.argv[6]        if len(sys.argv) > 6 else "OneMax"
energia_max  = float(sys.argv[7]) if len(sys.argv) > 7 else 0.0   # 0 = sin limite 

if temp_max > 0:     print(f"[Python] Temperatura maxima: {temp_max} C")
print(f"[Python] Problema:           {problema}")
if potencia_max > 0: print(f"[Python] Potencia maxima:    {potencia_max:.0f} W")
if tiempo_max   > 0: print(f"[Python] Tiempo maximo:      {tiempo_max:.0f} s")
if energia_max  > 0: print(f"[Python] Energia maxima:     {energia_max:.4f} Wh")


java_proceso     = None
powerlog_proceso = None
parada_por_temp     = False
parada_por_potencia = False
parada_por_tiempo   = False
parada_por_energia  = False

muestras_temperatura = []
lock_muestras        = threading.Lock()
t_inicio_medicion    = None
energia_acumulada_wh = 0.0


def leer_temperatura_powerlog(csv_path):
    if not os.path.exists(csv_path):
        return None

    try:
        with open(csv_path, "r", encoding="utf-8", errors="replace") as f:
            lineas = f.readlines()

        if len(lineas) < 2:
            return None

        cabecera = lineas[0].strip().split(",")
        idx_temp = None
        for i, col in enumerate(cabecera):
            if "Package Temperature" in col:
                idx_temp = i
                break

        if idx_temp is None:
            return None

        for linea in reversed(lineas[1:]):
            linea = linea.strip()
            if not linea or linea.startswith("Total"):
                continue
            partes = linea.split(",")
            if idx_temp < len(partes):
                try:
                    return float(partes[idx_temp])
                except ValueError:
                    continue

    except Exception:
        pass

    return None



def leer_temperatura_linux():
    try:
        import glob
        zonas = sorted(glob.glob("/sys/class/thermal/thermal_zone*/temp"))
        temps = []
        for zona in zonas:
            try:
                with open(zona, "r") as f:
                    val = int(f.read().strip()) / 1000.0
                    if val > 0 and val < 120:
                        temps.append(val)
            except Exception:
                continue
        if temps:
            return max(temps)
    except Exception:
        pass
    return None


def leer_potencia_linux():
    if not PSUTIL_DISPONIBLE:
        return None
    try:
        # Estimacion basada en uso de CPU: potencia_base * uso%
        cpu_percent = psutil.cpu_percent(interval=0.1)
        cpu_count = psutil.cpu_count()
        # Estimacion conservadora: ~5W base + uso proporcional hasta ~65W
        potencia_estimada = 5.0 + (cpu_percent / 100.0) * 60.0
        return round(potencia_estimada, 2)
    except Exception:
        return None


_esperando_impreso = [False]

def _parar_java(motivo):
    print(f"[Monitor] PARADA: {motivo}")
    print("[Monitor] Terminando proceso Java...")
    try:
        java_proceso.terminate()
        time.sleep(3)
        if java_proceso.poll() is None:
            java_proceso.kill()
    except Exception as e:
        print(f"[Monitor] Error al terminar Java: {e}")


def leer_potencia_powerlog(csv_path):
    if not os.path.exists(csv_path):
        return None
    try:
        with open(csv_path, "r", encoding="utf-8", errors="replace") as f:
            lineas = f.readlines()
        if len(lineas) < 2:
            return None
        cabecera = lineas[0].strip().split(",")
        idx_pot = None
        for i, col in enumerate(cabecera):
            if "Processor Power" in col or "Package Power" in col:
                idx_pot = i
                break
        if idx_pot is None:
            return None
        for linea in reversed(lineas[1:]):
            linea = linea.strip()
            if not linea or linea.startswith("Total"):
                continue
            partes = linea.split(",")
            if idx_pot < len(partes):
                try:
                    return float(partes[idx_pot])
                except ValueError:
                    continue
    except Exception:
        pass
    return None


def monitor_temperatura():
    global parada_por_temp, parada_por_potencia, parada_por_tiempo
    _esperando_impreso[0] = False
    tiempo_inicio = time.time()

    time.sleep(2.0)

    INTERVALO_MUESTRA  = 0.25
    INTERVALO_PANTALLA = 1.0
    ultimo_print       = 0.0

    t_monitor_inicio = time.time()

    while True:
        if java_proceso is None or java_proceso.poll() is not None:
            break

        if ES_WINDOWS:
            temp = leer_temperatura_powerlog(CSV_POWERLOG)
        else:
            temp = leer_temperatura_linux()
        ahora = time.time()

        if temp is not None:
            tiempo_relativo = ahora - t_inicio_medicion if t_inicio_medicion else 0.0
            if ES_WINDOWS:
                potencia_actual = leer_potencia_powerlog(CSV_POWERLOG)
            else:
                potencia_actual = leer_potencia_linux()
            with lock_muestras:
                muestras_temperatura.append({
                    "tiempo":   round(tiempo_relativo, 3),
                    "temp":     temp,
                    "potencia": round(potencia_actual, 2) if potencia_actual is not None else None
                })

            tiempo_monitor = ahora - t_monitor_inicio
            if tiempo_monitor - ultimo_print >= INTERVALO_PANTALLA:
                if temp_max > 0:
                    print(f"[Monitor] Temperatura CPU: {temp:.1f} C  (limite: {temp_max} C)")
                else:
                    print(f"[Monitor] Temperatura CPU: {temp:.1f} C")
                ultimo_print = tiempo_monitor

            if temp_max > 0 and temp > temp_max:
                parada_por_temp = True
                _parar_java(f"temperatura {temp:.1f} C supera el limite de {temp_max} C")
                break

        if potencia_max > 0:
            potencia = leer_potencia_powerlog(CSV_POWERLOG) if ES_WINDOWS else leer_potencia_linux()
            if potencia is not None and potencia > potencia_max:
                parada_por_potencia = True
                _parar_java(f"potencia {potencia:.1f} W supera el limite de {potencia_max:.0f} W")
                break

        # Acumular energia en tiempo real
        if energia_max > 0:
            global energia_acumulada_wh
            potencia_actual = leer_potencia_powerlog(CSV_POWERLOG) if ES_WINDOWS else leer_potencia_linux()
            if potencia_actual is not None:
                energia_acumulada_wh += potencia_actual * INTERVALO_MUESTRA / 3600.0
                if energia_acumulada_wh >= energia_max:
                    parada_por_energia = True
                    _parar_java(f"energia acumulada {energia_acumulada_wh:.4f} Wh supera el limite de {energia_max:.4f} Wh")
                    break

        if tiempo_max > 0:
            elapsed = time.time() - tiempo_inicio
            if elapsed > tiempo_max:
                parada_por_tiempo = True
                _parar_java(f"tiempo {elapsed:.1f} s supera el limite de {tiempo_max:.0f} s")
                break

        if temp is None:
            with lock_muestras:
                ya_hay_muestras = len(muestras_temperatura) > 0
            if not ya_hay_muestras and not _esperando_impreso[0]:
                print("[Monitor] Esperando datos de temperatura...")
                _esperando_impreso[0] = True

        time.sleep(INTERVALO_MUESTRA)

    with lock_muestras:
        n = len(muestras_temperatura)


def guardar_temperaturas_json():
    with lock_muestras:
        copia = list(muestras_temperatura)

    if not copia:
        print("[Monitor] AVISO: no hay muestras de temperatura para guardar.")
        return

    with open(JSON_TEMPERATURAS, "w", encoding="utf-8") as f:
        json.dump(copia, f)

    temps = [m["temp"] for m in copia]
    watts = [m["potencia"] for m in copia if m.get("potencia") is not None]
    print(f"[Monitor] Datos guardados en {JSON_TEMPERATURAS}")
    print(f"[Monitor]   Temp:     {len(copia)} muestras  |  Min: {min(temps):.1f}C  Max: {max(temps):.1f}C  Media: {sum(temps)/len(temps):.1f}C")
    if watts:
        print(f"[Monitor]   Potencia: {len(watts)} muestras  |  Min: {min(watts):.1f}W  Max: {max(watts):.1f}W  Media: {sum(watts)/len(watts):.1f}W")


def ejecutar_java():
    global java_proceso, t_inicio_medicion

    separador = ";" if os.name == "nt" else ":"
    classpath = separador.join([
        "libs/ea-1.0.jar",
        "libs/json-simple-4.0.1.jar",
        "libs/slf4j-api-2.0.13.jar",
        "libs/slf4j-simple-2.0.13.jar",
        "target/classes"
    ])

    comando_java = [
        "java", "-cp", classpath,
        "ejecucion.EjecutorAE",
        str(num_runs), str(seed), str(temp_max),
        str(potencia_max), str(tiempo_max),
        problema
    ]

    print(f"[Java] Lanzando proceso...")

    env_java = os.environ.copy()
    env_java["LAUNCHED_BY_PYTHON"] = "1"

    t_inicio_medicion = time.time()

    java_proceso = subprocess.Popen(
        comando_java,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        env=env_java
    )

    for line in java_proceso.stdout:
        print(f"[Java] {line.rstrip()}")

    java_proceso.wait()
    codigo = java_proceso.returncode
    print(f"[Java] Finalizado con codigo {codigo}")
    return codigo


def ejecutar_powerlog():
    global powerlog_proceso

    if not os.path.exists(POWERLOG_PATH):
        print("[PowerGadget] No encontrado en: " + POWERLOG_PATH)
        print("[PowerGadget] Omitiendo registro de temperatura.")
        return

    if os.path.exists(CSV_POWERLOG):
        try:
            os.remove(CSV_POWERLOG)
        except Exception:
            pass

    comando = [POWERLOG_PATH, "-duration", "3600", "-file", CSV_POWERLOG]
    print("[PowerGadget] Iniciando registro en tiempo real...")
    powerlog_proceso = subprocess.Popen(
        comando,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )


print("=" * 55)
print("Iniciando medicion...")
print("=" * 55)

# 1. Arrancar Power Gadget (solo Windows)
power_gadget_disponible = ES_WINDOWS and os.path.exists(POWERLOG_PATH)
if ES_WINDOWS:
    if power_gadget_disponible:
        powerlog_thread = threading.Thread(target=ejecutar_powerlog, daemon=True)
        powerlog_thread.start()
        time.sleep(1.5)
    else:
        print("[PowerGadget] No instalado. La parada por temperatura no estara disponible.")
else:
    print("[Linux] Usando /sys/class/thermal para temperatura" + (" y psutil para potencia." if PSUTIL_DISPONIBLE else 
    ". psutil no disponible, potencia no medida."))

# 2. Arrancar CodeCarbon
import logging
logging.getLogger("codecarbon").setLevel(logging.ERROR)

tracker = EmissionsTracker(
    output_dir=".",
    output_file="codecarbon.csv",
    save_to_file=True,
    log_level="error"
)
tracker.start()

# 3. Arrancar monitor (Windows con Power Gadget, o Linux con /sys/class/thermal)
if power_gadget_disponible or ES_LINUX:
    monitor_thread = threading.Thread(target=monitor_temperatura, daemon=True)
    monitor_thread.start()

# 4. Ejecutar Java 
ret_java = ejecutar_java()

# 5. Parar CodeCarbon
tracker.stop()

# 6. Guardar temperaturas acumuladas en JSON
guardar_temperaturas_json()

# 7. Parar Power Gadget (solo Windows)
if ES_WINDOWS and powerlog_proceso and powerlog_proceso.poll() is None:
    time.sleep(3)
    powerlog_proceso.terminate()
    try:
        powerlog_proceso.wait(timeout=5)
    except subprocess.TimeoutExpired:
        powerlog_proceso.kill()

# 8. Resumen final
print("=" * 55)
if parada_por_temp:
    print("[CodeCarbon] Medicion detenida: temperatura excesiva.")
elif parada_por_potencia:
    print("[CodeCarbon] Medicion detenida: potencia instantanea excesiva.")
elif parada_por_tiempo:
    print("[CodeCarbon] Medicion detenida: tiempo maximo alcanzado.")
elif parada_por_energia:
    print(f"[CodeCarbon] Medicion detenida: energia maxima alcanzada ({energia_acumulada_wh:.4f} Wh).")
else:
    print("[CodeCarbon] Medicion finalizada correctamente.")
print("[CodeCarbon] Datos guardados en codecarbon.csv")
print("=" * 55)
sys.exit(0)