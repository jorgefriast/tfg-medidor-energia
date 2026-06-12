package analisis;

import java.io.*;
import java.util.*;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class FiltradorCSV {

    private static final List<String> COLUMNAS_KEPT = Arrays.asList(
        "run_id",
        "experiment_id",
        "duration",
        "emissions",
        "emissions_rate",
        "cpu_power",
        "gpu_power",
        "ram_power",
        "cpu_energy",
        "gpu_energy",
        "ram_energy",
        "energy_consumed",
        "country_iso_code",
        "region",
        "os",
        "cpu_count",
        "cpu_model",
        "gpu_count",
        "gpu_model"
    );

    public void filtrarConTemperatura(String codecarbonPath,
                                      String powerGadgetPath,   
                                      String salidaPath,
                                      int numRuns,
                                      String problema,
                                      int descansoSeg) throws IOException {

        // Leemos las muestras del JSON generado por el monitor Python
        // en vez del CSV de Intel Power Gadget (que quedaba truncado)
        List<MuestraTemperatura> muestras = leerMuestrasDesdeJson("temps_monitor.json");

        if (muestras.isEmpty()) {
            // Fallback: si no hay JSON intentamos el CSV de Power Gadget como antes
            System.out.println("[FiltradorCSV] AVISO: no se encontro temps_monitor.json, intentando CSV de Power Gadget...");
            muestras = leerMuestrasDesdeCSV(powerGadgetPath);
        }

        if (muestras.isEmpty()) {
            throw new IOException("No se pudieron leer muestras de temperatura de ninguna fuente.");
        }

        System.out.println("[FiltradorCSV] Muestras de temperatura cargadas: " + muestras.size());

        File archivoSalida = new File(salidaPath);
        boolean archivoExiste = archivoSalida.exists();

        try (BufferedReader br = new BufferedReader(new FileReader(codecarbonPath));
             PrintWriter pw = new PrintWriter(new FileWriter(salidaPath, true))) {

            String header = br.readLine();
            if (header == null) {
                throw new IOException("CSV de CodeCarbon vacio");
            }

            String[] columnas = header.split(",", -1);
            Map<String, Integer> indicePorNombre = new HashMap<>();
            for (int i = 0; i < columnas.length; i++) {
                indicePorNombre.put(columnas[i].trim(), i);
            }

            List<Integer> indices = new ArrayList<>();
            for (String col : COLUMNAS_KEPT) {
                Integer idx = indicePorNombre.get(col);
                if (idx != null) {
                    indices.add(idx);
                }
            }

            Integer idxDuration = indicePorNombre.get("duration");
            if (idxDuration == null) {
                throw new IOException("No se encontro columna duration");
            }

            if (!archivoExiste) {
                pw.println(String.join(",", COLUMNAS_KEPT)
                        + ",duration_hours,potencia_media,porc_cpu,porc_ram,porc_gpu,temperatura,num_runs,problema,descanso_seg");
            }

            double tiempoAcumulado = 0.0;
            String line;

            while ((line = br.readLine()) != null) {
                String[] valores = line.split(",", -1);

                List<String> seleccionados = new ArrayList<>();
                for (int idx : indices) {
                    if (idx < valores.length) {
                        seleccionados.add(valores[idx]);
                    } else {
                        seleccionados.add("");
                    }
                }

                try {
                    double duration     = Double.parseDouble(valores[indicePorNombre.get("duration")]);
                    double energyConsumed = Double.parseDouble(valores[indicePorNombre.get("energy_consumed")]);
                    double cpuEnergy    = Double.parseDouble(valores[indicePorNombre.get("cpu_energy")]);
                    double ramEnergy    = Double.parseDouble(valores[indicePorNombre.get("ram_energy")]);
                    double gpuEnergy    = Double.parseDouble(valores[indicePorNombre.get("gpu_energy")]);

                    double durationHoras = duration / 3600.0;
                    double potenciaMedia = (durationHoras > 0) ? (energyConsumed / durationHoras) * 1000.0 : 0.0;
                    double porcCpu = (energyConsumed > 0) ? cpuEnergy / energyConsumed * 100.0 : 0.0;
                    double porcRam = (energyConsumed > 0) ? ramEnergy / energyConsumed * 100.0 : 0.0;
                    double porcGpu = (energyConsumed > 0) ? gpuEnergy / energyConsumed * 100.0 : 0.0;

                    double temperatura = calcularTemperaturaPromedio(
                        muestras,
                        tiempoAcumulado,
                        tiempoAcumulado + duration
                    );

                    tiempoAcumulado += duration;

                    seleccionados.add(String.valueOf(durationHoras));
                    seleccionados.add(String.valueOf(potenciaMedia));
                    seleccionados.add(String.valueOf(porcCpu));
                    seleccionados.add(String.valueOf(porcRam));
                    seleccionados.add(String.valueOf(porcGpu));
                    seleccionados.add(String.valueOf(temperatura));
                    seleccionados.add(String.valueOf(numRuns));
                    seleccionados.add(problema);
                    seleccionados.add(String.valueOf(descansoSeg));

                } catch (Exception e) {
                    seleccionados.add("");
                    seleccionados.add("");
                    seleccionados.add("");
                    seleccionados.add("");
                    seleccionados.add("");
                    seleccionados.add("");
                    seleccionados.add(String.valueOf(numRuns));
                    seleccionados.add(problema);
                    seleccionados.add(String.valueOf(descansoSeg));
                }

                pw.println(String.join(",", seleccionados));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Lee muestras desde el JSON generado por el monitor Python.
    // Formato esperado: [{"tiempo": 0.25, "temp": 53.0, "potencia": 16.7}, ...]
    // -----------------------------------------------------------------------
    private List<MuestraTemperatura> leerMuestrasDesdeJson(String jsonPath) {
        List<MuestraTemperatura> muestras = new ArrayList<>();

        File f = new File(jsonPath);
        if (!f.exists()) return muestras;

        try (FileReader reader = new FileReader(f)) {
            // Usamos json-simple (misma librería que EjecutorAE) para parsear
            // el array JSON de forma segura, sin manipulación manual de strings.
            JsonArray array = (JsonArray) Jsoner.deserialize(reader);

            for (Object elemento : array) {
                JsonObject obj = (JsonObject) elemento;

                Number tiempo = (Number) obj.get("tiempo");
                Number temp   = (Number) obj.get("temp");

                if (tiempo != null && temp != null) {
                    muestras.add(new MuestraTemperatura(
                        tiempo.doubleValue(),
                        temp.doubleValue()
                    ));
                }
            }

        } catch (Exception e) {
            System.out.println("[FiltradorCSV] Error leyendo JSON de temperaturas: " + e.getMessage());
        }

        return muestras;
    }

    // -----------------------------------------------------------------------
    // Fallback: lee muestras desde el CSV de Intel Power Gadget (comportamiento original)
    // -----------------------------------------------------------------------
    private List<MuestraTemperatura> leerMuestrasDesdeCSV(String powerGadgetPath) {
        List<MuestraTemperatura> muestras = new ArrayList<>();

        File f = new File(powerGadgetPath);
        if (!f.exists()) return muestras;

        try (BufferedReader brPG = new BufferedReader(new FileReader(f))) {
            String headerPG = brPG.readLine();
            if (headerPG == null) return muestras;

            String[] columnasPG = headerPG.split(",", -1);
            int idxTemp = -1;
            int idxTime = -1;

            for (int i = 0; i < columnasPG.length; i++) {
                String col = columnasPG[i].trim();
                if (col.contains("Package Temperature")) idxTemp = i;
                if (col.contains("Elapsed Time"))        idxTime = i;
            }

            if (idxTemp == -1 || idxTime == -1) return muestras;

            String linePG;
            while ((linePG = brPG.readLine()) != null) {
                if (linePG.trim().isEmpty() || linePG.startsWith("Total")) break;
                String[] valores = linePG.split(",", -1);
                if (idxTemp < valores.length && idxTime < valores.length) {
                    try {
                        double tiempo = Double.parseDouble(valores[idxTime].trim());
                        double temp   = Double.parseDouble(valores[idxTemp].trim());
                        muestras.add(new MuestraTemperatura(tiempo, temp));
                    } catch (NumberFormatException ignore) {}
                }
            }
        } catch (Exception e) {
            System.out.println("[FiltradorCSV] Error leyendo CSV de Power Gadget: " + e.getMessage());
        }

        return muestras;
    }

    // -----------------------------------------------------------------------
    // Clases y métodos de cálculo (sin cambios)
    // -----------------------------------------------------------------------
    private static class MuestraTemperatura {
        double tiempo;
        double temperatura;

        MuestraTemperatura(double tiempo, double temperatura) {
            this.tiempo      = tiempo;
            this.temperatura = temperatura;
        }
    }

    private double calcularTemperaturaPromedio(List<MuestraTemperatura> muestras,
                                               double tiempoInicio,
                                               double tiempoFin) {
        List<Double> temperaturasEnRango = new ArrayList<>();

        for (MuestraTemperatura m : muestras) {
            if (m.tiempo >= tiempoInicio && m.tiempo <= tiempoFin) {
                temperaturasEnRango.add(m.temperatura);
            }
        }

        if (temperaturasEnRango.isEmpty()) {
            return buscarTemperaturaMasCercana(muestras, (tiempoInicio + tiempoFin) / 2);
        }

        double suma = 0.0;
        for (double t : temperaturasEnRango) suma += t;
        return suma / temperaturasEnRango.size();
    }

    private double buscarTemperaturaMasCercana(List<MuestraTemperatura> muestras, double tiempo) {
        if (muestras.isEmpty()) return 0.0;

        double menorDiferencia   = Double.MAX_VALUE;
        double temperaturaCercana = muestras.get(0).temperatura;

        for (MuestraTemperatura m : muestras) {
            double diferencia = Math.abs(m.tiempo - tiempo);
            if (diferencia < menorDiferencia) {
                menorDiferencia    = diferencia;
                temperaturaCercana = m.temperatura;
            }
        }

        return temperaturaCercana;
    }

    // -----------------------------------------------------------------------
    // Version sin Power Gadget (Linux): usa temps_monitor.json si existe,
    // y si no, escribe -1 en temperatura y potencia.
    // -----------------------------------------------------------------------
    public void filtrarSinTemperatura(String codecarbonPath,
                                      String salidaPath,
                                      int numRuns,
                                      String problema,
                                      int descansoSeg) throws IOException {

        // Intentamos igualmente leer el JSON del monitor (temperatura via /sys/class/thermal)
        List<MuestraTemperatura> muestras = leerMuestrasDesdeJson("temps_monitor.json");
        boolean hayMuestras = !muestras.isEmpty();

        if (hayMuestras) {
            System.out.println("[FiltradorCSV] Muestras del monitor cargadas: " + muestras.size());
        } else {
            System.out.println("[FiltradorCSV] Sin muestras de temperatura, usando -1.");
        }

        File archivoSalida = new File(salidaPath);
        boolean archivoExiste = archivoSalida.exists();

        try (BufferedReader br = new BufferedReader(new FileReader(codecarbonPath));
             PrintWriter pw = new PrintWriter(new FileWriter(salidaPath, true))) {

            String header = br.readLine();
            if (header == null) throw new IOException("CSV de CodeCarbon vacio");

            String[] columnas = header.split(",", -1);
            Map<String, Integer> indicePorNombre = new HashMap<>();
            for (int i = 0; i < columnas.length; i++) {
                indicePorNombre.put(columnas[i].trim(), i);
            }

            List<Integer> indices = new ArrayList<>();
            for (String col : COLUMNAS_KEPT) {
                Integer idx = indicePorNombre.get(col);
                if (idx != null) indices.add(idx);
            }

            if (!archivoExiste) {
                pw.println(String.join(",", COLUMNAS_KEPT)
                        + ",duration_hours,potencia_media,porc_cpu,porc_ram,porc_gpu,temperatura,num_runs,problema,descanso_seg");
            }

            double tiempoAcumulado = 0.0;
            String line;

            while ((line = br.readLine()) != null) {
                String[] valores = line.split(",", -1);

                List<String> seleccionados = new ArrayList<>();
                for (int idx : indices) {
                    seleccionados.add(idx < valores.length ? valores[idx] : "");
                }

                try {
                    double duration      = Double.parseDouble(valores[indicePorNombre.get("duration")]);
                    double energyConsumed = Double.parseDouble(valores[indicePorNombre.get("energy_consumed")]);
                    double cpuEnergy     = Double.parseDouble(valores[indicePorNombre.get("cpu_energy")]);
                    double ramEnergy     = Double.parseDouble(valores[indicePorNombre.get("ram_energy")]);
                    double gpuEnergy     = Double.parseDouble(valores[indicePorNombre.get("gpu_energy")]);

                    double durationHoras = duration / 3600.0;
                    double potenciaMedia = (durationHoras > 0) ? (energyConsumed / durationHoras) * 1000.0 : 0.0;
                    double porcCpu = (energyConsumed > 0) ? cpuEnergy / energyConsumed * 100.0 : 0.0;
                    double porcRam = (energyConsumed > 0) ? ramEnergy / energyConsumed * 100.0 : 0.0;
                    double porcGpu = (energyConsumed > 0) ? gpuEnergy / energyConsumed * 100.0 : 0.0;

                    double temperatura;
                    if (hayMuestras) {
                        temperatura = calcularTemperaturaPromedio(muestras, tiempoAcumulado, tiempoAcumulado + duration);
                    } else {
                        temperatura = -1.0;
                    }
                    tiempoAcumulado += duration;

                    pw.println(String.join(",", seleccionados)
                            + "," + String.format("%.6f", durationHoras)
                            + "," + String.format("%.4f", potenciaMedia)
                            + "," + String.format("%.2f", porcCpu)
                            + "," + String.format("%.2f", porcRam)
                            + "," + String.format("%.2f", porcGpu)
                            + "," + String.format("%.2f", temperatura)
                            + "," + numRuns
                            + "," + problema
                            + "," + descansoSeg);

                } catch (NumberFormatException e) {
                    System.out.println("[FiltradorCSV] Fila ignorada: " + e.getMessage());
                }
            }
        }
    }
}