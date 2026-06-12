package medicion;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import analisis.FiltradorCSV;

public class MedidorEnergia {

    private String csvPath = "codecarbon.csv";
    private String csvFinal = "codecarbon_final.csv";
    private java.util.function.Consumer<String> logger;
    // Ponemos el numRuns a 1 por defecto
    private int numRuns = 1;
    private String problema = "OneMax";
    private int descansoAntes = 0;

    public MedidorEnergia(java.util.function.Consumer<String> logger) {
    	// El log de la ventana, para escribir
        this.logger = (logger != null) ? logger : System.out::println;
    }

    private void log(String msg) {
        logger.accept(msg);
    }

    public void iniciarMedicion(int numRuns, int seed, String problema, double tempMaxima, int potenciaMax, int tiempoMax, double energiaMax, int descansoAntes) {
    	// Guarda el número de runs y escribe en la consola que va a empezar.
        this.numRuns = numRuns;
        this.problema = problema;
        this.descansoAntes = descansoAntes;
        log("Arrancamos CodeCarbon");

        // Para saber donde esta python
        String pythonCommand = System.getenv("CODECARBON_PYTHON");
        if (pythonCommand == null || pythonCommand.isEmpty()) {
            pythonCommand = detectarPython();
        }

        log("[Python] Usando: " + pythonCommand);
        if (tempMaxima > 0) log("[Python] Temperatura máxima: " + tempMaxima + "°C");
        if (potenciaMax > 0) log("[Python] Potencia máxima: " + potenciaMax + " W");
        if (tiempoMax   > 0) log("[Python] Tiempo máximo: "   + tiempoMax   + " s");
        if (energiaMax  > 0) log("[Python] Energía máxima: "  + energiaMax  + " Wh");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                pythonCommand,
                "-u",
                "medir_codecarbon.py",
                String.valueOf(numRuns),
                String.valueOf(seed),
                String.valueOf(tempMaxima),
                String.valueOf(potenciaMax),
                String.valueOf(tiempoMax),
                problema,
                String.valueOf(energiaMax)
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            // Esto es para que cada línea que codecarbon escriba, la mandemos a la
            // interfaz gráfica.
            String line;
            while ((line = reader.readLine()) != null) {
                log("[Script] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log("Script ejecutado correctamente");
            } else {
                log("Script terminó con código " + exitCode);
            }
        } catch (IOException e) {
            log("Error al ejecutar el script de CodeCarbon:");
            log("  " + e.getMessage());
            log("  Verifica que Python y CodeCarbon estén instalados");
        } catch (InterruptedException e) {
            log("Proceso interrumpido: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método de apoyo por si no encuentra python, prueba rutas en las que suele estar
    // dependiendo del sistema operativo
    private String detectarPython() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            String[] opciones = {
                userHome + "\\miniconda3\\envs\\codecarbon\\python.exe",
                userHome + "\\anaconda3\\envs\\codecarbon\\python.exe",
                "python",
                "python3",
                "py"
            };

            for (String cmd : opciones) {
                if (pythonExiste(cmd)) {
                    return cmd;
                }
            }
        } else {
            String[] opciones = {
                userHome + "/codecarbon-env/bin/python",
                userHome + "/codecarbon-env/bin/python3",
                userHome + "/miniconda3/envs/codecarbon/bin/python",
                userHome + "/anaconda3/envs/codecarbon/bin/python",
                "python3",
                "python"
            };
            for (String cmd : opciones) {
                if (pythonExiste(cmd)) {
                    return cmd;
                }
            }
        }

        return "python";
    }

    private boolean pythonExiste(String comando) {
        try {
            Process p = new ProcessBuilder(comando, "--version")
                .redirectErrorStream(true)
                .start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void finalizarMedicion() {
        log("Procesando resultados finales...");
        try {
            File fCodeCarbon = new File(csvPath);
            boolean esWindows = System.getProperty("os.name").toLowerCase().contains("win");
            File fPowerGadget = new File("intel_power_gadget_log.csv");

            // En Windows necesitamos ambos archivos; en Linux solo CodeCarbon
            if (!fCodeCarbon.exists()) {
                log("ERROR: Faltan archivos.");
                return;
            }
            if (esWindows && !fPowerGadget.exists()) {
                log("ERROR: Falta el log de Intel Power Gadget.");
                return;
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Guardar copia del log de Intel solo en Windows
            if (esWindows && fPowerGadget.exists()) {
                File carpetaLogs = new File("intel_logs");
                if (!carpetaLogs.exists()) {
                    carpetaLogs.mkdirs();
                    log("Carpeta creada: intel_logs/");
                }
                String nombreCopia = "intel_logs/intel_power_gadget_log_" + timestamp + ".csv";
                Files.copy(fPowerGadget.toPath(), Paths.get(nombreCopia), StandardCopyOption.REPLACE_EXISTING);
                log("Log de Intel guardado en: " + nombreCopia);
            }

            // Guardar copia del JSON del monitor con el mismo timestamp
            File fMonitor = new File("temps_monitor.json");
            if (fMonitor.exists()) {
                File carpetaMonitor = new File("monitor_logs");
                if (!carpetaMonitor.exists()) carpetaMonitor.mkdirs();
                String nombreJson = "monitor_logs/monitor_log_" + timestamp + ".json";
                Files.copy(fMonitor.toPath(), Paths.get(nombreJson), StandardCopyOption.REPLACE_EXISTING);
                log("Monitor log guardado en: " + nombreJson);
            }

            FiltradorCSV filtrador = new FiltradorCSV();

            // En Windows fusionamos con el log de Intel y en Linux solo procesamos CodeCarbon
            if (esWindows && fPowerGadget.exists()) {
                filtrador.filtrarConTemperatura(csvPath, fPowerGadget.getName(), csvFinal, numRuns, problema, descansoAntes);
                eliminarArchivoTemporal(fPowerGadget.getName());
            } else {
                filtrador.filtrarSinTemperatura(csvPath, csvFinal, numRuns, problema, descansoAntes);
            }

            log("Análisis completado: " + csvFinal);

            eliminarArchivoTemporal(csvPath);

        } catch (Exception e) {
            log("Error al procesar los resultados: " + e.getMessage());
        }
    }

    private void eliminarArchivoTemporal(String archivo) {
        try {
            File f = new File(archivo);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
        }
    }

    public String getResultados() {
        return csvFinal;
    }
}