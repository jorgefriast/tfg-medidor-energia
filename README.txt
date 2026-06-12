# TFG - Medición de Consumo Energético en Algoritmos Evolutivos

## Descripción

Este proyecto implementa un sistema de medición del consumo energético de algoritmos evolutivos utilizando CodeCarbon para mediciones a nivel de software e Intel Power Gadget para mediciones de hardware.

El sistema está desarrollado en Java con integración de Python para las herramientas de medición, y proporciona una interfaz gráfica para ejecutar las mediciones y visualizar los resultados en tiempo real.

## Características principales

- Medición automática de consumo energético durante la ejecución de algoritmos evolutivos
- Registro de consumo por componente (CPU, RAM, GPU)
- Captura de temperatura del procesador
- Filtrado y procesamiento automático de datos
- Cálculo de métricas derivadas (potencia media, porcentajes de consumo, etc.)
- Interfaz gráfica para facilitar la ejecución de experimentos

## Requisitos

### Software necesario

- Java 17 o superior
- Python 3.8 o superior
- CodeCarbon (instalado en un entorno Python)
- Intel Power Gadget 3.6 (solo Windows, opcional)
- Maven (para compilación)

### Instalación de dependencias

Instalar CodeCarbon en un entorno Python:
```bash
conda create -n codecarbon python=3.9
conda activate codecarbon
pip install codecarbon
```

## Configuración

### Variables de entorno

El sistema requiere configurar la ruta al intérprete de Python que tiene CodeCarbon instalado.

#### Configuración en Eclipse

1. Click derecho en el proyecto
2. Seleccionar `Run As` > `Run Configurations`
3. En la pestaña `Environment`, hacer click en `New`
4. Añadir las siguientes variables:

**Variable obligatoria:**
- Name: `CODECARBON_PYTHON`
- Value: Ruta completa al ejecutable de Python
  - Ejemplo Windows: `C:\Users\TuUsuario\miniconda3\envs\codecarbon\python.exe`
  - Ejemplo Linux/Mac: `/home/usuario/miniconda3/envs/codecarbon/bin/python`

**Variable opcional (solo Windows con Power Gadget):**
- Name: `POWERLOG_PATH`
- Value: `C:\Program Files\Intel\Power Gadget 3.6\PowerLog3.0.exe`

5. Click en `Apply` y luego `Run`

#### Configuración desde terminal

**Windows:**
```cmd
set CODECARBON_PYTHON=C:\Users\TuUsuario\miniconda3\envs\codecarbon\python.exe
```

**Linux/Mac:**
```bash
export CODECARBON_PYTHON=/home/usuario/miniconda3/envs/codecarbon/bin/python
```

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
│           │   └── FiltradorCSV.java        # Procesa los datos obtenidos
│           └── tfgmain/
│               ├── App.java                 # Punto de entrada
│               └── VentanaPrincipal.java    # Interfaz gráfica
├── libs/
│   ├── ea-1.0.jar                           # Biblioteca de algoritmos evolutivos
│   └── json-simple-4.0.1.jar
├── medir_codecarbon.py                      # Script de medición
└── pom.xml
```

## Uso

### Compilación
```bash
mvn clean package
```

### Ejecución

Ejecutar desde Eclipse o mediante:
```bash
java -cp "libs/ea-1.0.jar;libs/json-simple-4.0.1.jar;target/tfg-0.0.1-SNAPSHOT.jar" tfgmain.App
```

Nota: En Linux/Mac usar `:` en lugar de `;` como separador.

### Proceso de medición

1. Iniciar la aplicación
2. Hacer click en "Iniciar medición"
3. El sistema ejecutará automáticamente:
   - CodeCarbon para medir consumo energético
   - Intel Power Gadget para medir temperatura (si está disponible)
   - El algoritmo evolutivo configurado
4. Al finalizar, se generarán los archivos de resultados

## Archivos de salida

- `codecarbon.csv`: Datos brutos de CodeCarbon
- `codecarbon_filtrado.csv`: Datos filtrados con métricas calculadas
- `codecarbon_filtrado_temp.csv`: Datos filtrados con temperatura incluida
- `intel_power_gadget_log.csv`: Registro de Power Gadget (si está disponible)

## Métricas calculadas

El sistema calcula automáticamente:

- Duración en horas
- Potencia media (W)
- Porcentaje de consumo por CPU
- Porcentaje de consumo por RAM
- Porcentaje de consumo por GPU
- Temperatura media del procesador
- Temperatura máxima del procesador

## Solución de problemas

### Error: "ModuleNotFoundError: No module named 'codecarbon'"

El sistema está usando un Python que no tiene CodeCarbon instalado. Verificar que la variable `CODECARBON_PYTHON` apunta al Python correcto.

### Error: "PowerLog no encontrado"

Si no se dispone de Intel Power Gadget, el sistema continuará funcionando pero sin datos de temperatura. Esto es normal en sistemas Linux/Mac o Windows sin Power Gadget instalado.

## Autor

Trabajo Fin de Grado - Universidad de Málaga

## Licencia

Este proyecto es parte de un Trabajo Fin de Grado académico.