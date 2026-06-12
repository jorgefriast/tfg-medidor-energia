# TFG - Medición de Consumo Energético en Algoritmos Evolutivos

## Descripción

Este proyecto implementa un sistema de medición del consumo energético de algoritmos evolutivos utilizando CodeCarbon para mediciones a nivel de software e Intel Power Gadget para mediciones de hardware en Windows.

El sistema está desarrollado principalmente en Java, con un script auxiliar en Python que coordina las herramientas de medición externas, y proporciona una interfaz gráfica para configurar y ejecutar las mediciones, visualizando los resultados en gráficas estadísticas al finalizar.

## Características principales

- Medición automática de consumo energético durante la ejecución de algoritmos evolutivos
- Registro de consumo por componente (CPU, RAM, GPU)
- Captura de temperatura y potencia instantánea del procesador
- Cuatro condiciones de parada configurables (energía, tiempo, temperatura, potencia) que detienen el algoritmo de forma controlada sin perder las métricas
- Fusión y procesamiento automático de datos en un único fichero acumulado
- Cálculo de métricas derivadas (potencia media, porcentajes de consumo por componente, temperatura media, etc.)
- Interfaz gráfica para configurar experimentos y visualizar resultados en pestañas
- Soporte para problemas estándar (OneMax, LeadingOnes, DeceptiveTrap, MMDP) y para problemas definidos por el usuario mediante carga dinámica con Reflection (por ejemplo, el problema de la Mochila)

## Requisitos

### Software necesario

- Java Development Kit (JDK) 17 o superior
- Maven (gestión de dependencias y compilación)
- Python 3.10 o superior, con el paquete CodeCarbon instalado
- Intel Power Gadget (solo Windows, recomendado para obtener temperatura y potencia desde el hardware)

### Instalación de CodeCarbon

**Windows (entorno conda, recomendado):**
```bash
conda create -n codecarbon python=3.10
conda activate codecarbon
pip install codecarbon
```

**Linux (entorno virtual venv):**
```bash
python3 -m venv ~/codecarbon-env
source ~/codecarbon-env/bin/activate
pip install codecarbon psutil
```

## Configuración

El sistema necesita localizar un intérprete de Python que tenga CodeCarbon instalado. Esto se hace mediante la variable de entorno `CODECARBON_PYTHON`, o bien automáticamente si no se define.

### Windows

Si se ha creado un entorno conda llamado `codecarbon` (como se indica arriba), **es necesario** definir `CODECARBON_PYTHON`, ya que la detección automática no busca entornos conda por defecto.

**Configuración en Eclipse:**

1. Click derecho en el proyecto
2. Seleccionar `Run As` > `Run Configurations`
3. Seleccionar la configuración de la clase `App`
4. En la pestaña `Environment`, hacer click en `Add`
5. Añadir la variable:
   - Name: `CODECARBON_PYTHON`
   - Value: `C:\Users\TuUsuario\miniconda3\envs\codecarbon\python.exe`
6. Click en `Apply`

**Configuración desde terminal:**
```cmd
set CODECARBON_PYTHON=C:\Users\TuUsuario\miniconda3\envs\codecarbon\python.exe
```

### Linux

Si el entorno virtual se crea exactamente con el nombre y la ubicación `~/codecarbon-env` (como se indica arriba), **no es necesario** definir ninguna variable: el método `detectarPython()` lo localiza automáticamente.

Si se usa otro nombre, otra ubicación, o un entorno conda, definir explícitamente:
```bash
export CODECARBON_PYTHON=/home/usuario/miniconda3/envs/codecarbon/bin/python
```

> **Nota:** en máquinas virtuales, es posible que `/sys/class/thermal/` no exponga zonas térmicas (`thermal_zone*`). En ese caso, el monitor no registrará temperatura ni potencia para esa ejecución, pero la aplicación continuará funcionando con normalidad y generará igualmente el fichero de resultados con las métricas de CodeCarbon.

### Intel Power Gadget (Windows)

Intel declaró esta herramienta obsoleta a finales de 2023 y ya no está disponible en sus canales oficiales. Si se desea usar, descargar el instalador (versión 3.6) únicamente desde repositorios o espejos de confianza y analizarlo con un antivirus antes de ejecutarlo. Si no está instalado, la aplicación funciona igualmente, pero sin datos de temperatura ni potencia instantánea en Windows.

## Estructura del proyecto
```
tfg/
├── src/
│   └── main/
│       └── java/
│           ├── ejecucion/
│           │   └── EjecutorAE.java          # Ejecuta los algoritmos evolutivos
│           ├── medicion/
│           │   └── MedidorEnergia.java      # Coordina las mediciones
│           ├── analisis/
│           │   └── FiltradorCSV.java        # Fusiona y procesa los datos obtenidos
│           ├── problema/
│           │   └── Mochila.java             # Problema de ejemplo definido por el usuario
│           └── tfgmain/
│               ├── App.java                 # Punto de entrada
│               ├── VentanaPrincipal.java    # Interfaz gráfica principal
│               └── VentanaGraficas.java     # Ventana de resultados
├── libs/
│   ├── ea-1.0.jar                           # Biblioteca de algoritmos evolutivos (Caesium)
│   ├── json-simple-4.0.1.jar
│   ├── jSensors-2.2.1.jar
│   ├── slf4j-api-2.0.13.jar
│   └── slf4j-simple-2.0.13.jar
├── data/
│   └── mochila.dat                          # Datos de ejemplo para el problema de la Mochila
├── ea-config.json                           # Configuración del algoritmo evolutivo
├── medir_codecarbon.py                      # Script de medición y monitorización
└── pom.xml
```

## Uso

### Compilación
```bash
mvn clean package
```

### Ejecución

Ejecutar desde Eclipse (clase `App`) o mediante:
```bash
java -cp "libs/ea-1.0.jar;libs/json-simple-4.0.1.jar;libs/jSensors-2.2.1.jar;libs/slf4j-api-2.0.13.jar;libs/slf4j-simple-2.0.13.jar;target/tfg-0.0.1-SNAPSHOT.jar" tfgmain.App
```

> En Linux/Mac usar `:` en lugar de `;` como separador del classpath.

### Proceso de medición

1. Iniciar la aplicación (se abre `VentanaPrincipal`)
2. Configurar los parámetros: número de runs por medición, número de mediciones, descanso entre ellas, tipo de problema y condiciones de parada
3. (Opcional) Si se selecciona `UserProblem`, indicar la clase y el fichero de datos asociado
4. Pulsar **Iniciar medición**. El sistema ejecutará automáticamente:
   - El script `medir_codecarbon.py`, que arranca CodeCarbon
   - Intel Power Gadget (si está disponible, solo Windows)
   - El proceso Java con el algoritmo evolutivo configurado
   - Un hilo monitor que comprueba en tiempo real las condiciones de parada
5. Al finalizar (de forma natural o por alcanzar un umbral), los resultados se añaden a `codecarbon_final.csv`
6. Pulsar **Ver estadísticas** para abrir la ventana de gráficas con los resultados acumulados

## Archivos de salida

- `codecarbon.csv`: datos brutos generados por CodeCarbon en cada medición (temporal, se elimina al finalizar)
- `temps_monitor.json`: muestras de temperatura y potencia recogidas por el monitor durante la medición (temporal)
- `codecarbon_final.csv`: fichero acumulado con los datos fusionados y las métricas calculadas, una fila por ejecución. Es el fichero que utiliza la ventana de gráficas
- `stats.json`: estadísticas de *fitness* del algoritmo evolutivo de la última medición
- `intel_logs/` y `monitor_logs/`: copias archivadas con *timestamp* de los logs de cada medición (solo si Power Gadget está disponible / hay datos del monitor)

## Métricas calculadas

A partir de los datos de CodeCarbon y del monitor, el sistema calcula:

- Duración en horas
- Potencia media (W)
- Porcentaje de consumo por CPU
- Porcentaje de consumo por RAM
- Porcentaje de consumo por GPU
- Temperatura media del procesador durante cada ejecución
- Número de runs y nombre del problema ejecutado
- Segundos de descanso previos a cada medición

## Solución de problemas

### Error: "ModuleNotFoundError: No module named 'codecarbon'"

El sistema está usando un Python que no tiene CodeCarbon instalado. Verificar que la variable `CODECARBON_PYTHON` apunta al Python correcto, o que el entorno por defecto (`~/codecarbon-env` en Linux, o el indicado en `CODECARBON_PYTHON` en Windows) tiene CodeCarbon instalado.

### Sin datos de temperatura/potencia

Si no se dispone de Intel Power Gadget (Linux/Mac, o Windows sin instalarlo), o si en una máquina virtual Linux no se exponen las zonas térmicas (`/sys/class/thermal/thermal_zone*`), el sistema continuará funcionando con normalidad, pero esa medición no incluirá temperatura ni potencia instantánea. Esto es un comportamiento conocido, documentado en la memoria del TFG.

## Autor

Jorge Frías Tello — Trabajo Fin de Grado, Grado en Ingeniería Informática, Universidad de Málaga.
Tutorizado por Carlos Cotta Porras.

## Licencia

Este proyecto es parte de un Trabajo Fin de Grado académico.
