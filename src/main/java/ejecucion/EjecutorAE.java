package ejecucion;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Locale;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.github.cliftonlabs.json_simple.JsonArray;
import es.uma.lcc.caesium.ea.base.EvolutionaryAlgorithm;
import es.uma.lcc.caesium.ea.config.EAConfiguration;
import es.uma.lcc.caesium.ea.fitness.ObjectiveFunction;
import es.uma.lcc.caesium.ea.problem.discrete.binary.LeadingOnes;
import es.uma.lcc.caesium.ea.problem.discrete.binary.onemax.OneMax;
import es.uma.lcc.caesium.ea.problem.discrete.binary.trap.DeceptiveTrap;
import es.uma.lcc.caesium.ea.problem.discrete.binary.trap.MMDP;
import es.uma.lcc.caesium.ea.statistics.EntropyDiversity;
import es.uma.lcc.caesium.ea.test.MyVariationFactory;

/**
 * Módulo de ejecución del algoritmo evolutivo.
 *
 * Cuando se lanza desde medir_codecarbon.py, la parada por temperatura
 * la gestiona el monitor Python (que termina este proceso externamente).
 * El parámetro tempMaxima solo se usa si este módulo se ejecuta de forma
 * standalone (sin pasar por el script Python), como fallback en Linux.
 */
public class EjecutorAE {

    public void ejecutarAlgoritmo(int numRuns, long seed, String problema, double tempMaxima)
            throws FileNotFoundException, JsonException {

        System.out.println("Ejecutando Algoritmo Evolutivo...");

        EAConfiguration conf;
        try {
            FileReader reader = new FileReader("ea-config.json");
            conf = new EAConfiguration((JsonObject) Jsoner.deserialize(reader));
        } catch (FileNotFoundException e) {
            conf = new EAConfiguration();
        }

        conf.setSeed(seed);
        conf.setVariationFactory(new MyVariationFactory());
        conf.setNumRuns(1); // Ejecutamos run a run para poder guardar stats parciales

        System.out.println("seed:     " + conf.getSeed());
        System.out.println("islands:  " + conf.getNumIslands());

        // --- Modo standalone: comprobar si podemos leer temperatura localmente ---
        // Esto solo tiene efecto si se ejecuta sin el script Python (p.ej. en Linux).
        // Cuando Python controla el proceso, el monitor externo se encarga de parar.
        boolean modoStandalone = !estaControladoPorPython();
        if (modoStandalone) {
            System.out.println("[Temp] Modo standalone: parada por temperatura activada en Java.");
            System.out.println("[Temp] Temperatura maxima: " + tempMaxima + " C");
        }

        // --- Preparar algoritmo ---
        EvolutionaryAlgorithm myEA = new EvolutionaryAlgorithm(conf);
        // Instanciar el problema seleccionado por el usuario
        ObjectiveFunction funcion;
        if ("UserProblem".equals(problema)) {
            funcion = cargarProblemaUsuario();
        } else {
            switch (problema) {
                case "LeadingOnes":   funcion = new LeadingOnes(100);      break;
                case "DeceptiveTrap": funcion = new DeceptiveTrap(10, 10); break;
                case "MMDP":          funcion = new MMDP(16);              break;
                default:              funcion = new OneMax(100);           break;
            }
        }
        System.out.println("[Java] Problema: " + problema + " (" + funcion.getClass().getSimpleName() + ")");
        myEA.setObjectiveFunction(funcion);
        myEA.getStatistics().setDiversityMeasure(new EntropyDiversity());

        int runsCompletados = 0;

        for (int i = 0; i < numRuns; i++) {

            if (modoStandalone) {
                double tempActual = leerTemperaturaLinux();
                if (tempActual > 0.0) {
                    System.out.println(String.format(Locale.US,
                        "Run %d - Temperatura CPU: %.1f C", i, tempActual));
                    if (tempActual > tempMaxima) {
                        System.out.println(String.format(Locale.US,
                            "PARADA: %.1f C supera el limite de %.1f C. " +
                            "Completados %d/%d runs.",
                            tempActual, tempMaxima, runsCompletados, numRuns));
                        break;
                    }
                }
            } else {
                System.out.println("Iniciando run " + i + "...");
            }

            try {
                myEA.run();
            } catch (Exception e) {
                System.out.println("Run " + i + " interrumpido: " + e.getMessage());
                break;
            }

            runsCompletados++;

            System.out.println(String.format(Locale.US,
                "Run %d completado: %.2fs  fitness=%.4f",
                i,
                myEA.getStatistics().getTime(i),
                myEA.getStatistics().getBest(i).getFitness()));
        }

        if (runsCompletados > 0) {
            try (PrintWriter file = new PrintWriter("stats.json")) {
                file.print(myEA.getStatistics().toJSON().toJson());
            }
            System.out.println("Stats guardadas: " + runsCompletados + "/" + numRuns + " runs completados.");
        } else {
            System.out.println("Ningun run completado. stats.json no generado.");
        }
    }

    /**
     * Detecta si este proceso está siendo lanzado desde el script Python
     * comprobando si la variable de entorno CODECARBON_PYTHON está definida
     * o si hay un proceso padre que sea Python.
     *
     * Forma simple y fiable: el script Python define la variable de entorno
     * LAUNCHED_BY_PYTHON=1 al lanzar el proceso Java.
     */
    private boolean estaControladoPorPython() {
        return "1".equals(System.getenv("LAUNCHED_BY_PYTHON"));
    }

    /**
     * Fallback de lectura de temperatura para modo standalone en Linux.
     * Lee de /sys/class/thermal/thermal_zone0/temp (milésimas de °C).
     * Devuelve 0.0 si no se puede leer.
     */
    private double leerTemperaturaLinux() {
        try {
            java.io.File f = new java.io.File("/sys/class/thermal/thermal_zone0/temp");
            if (!f.exists()) return 0.0;
            try (BufferedReader br = new BufferedReader(new java.io.FileReader(f))) {
                String line = br.readLine();
                if (line != null) {
                    return Double.parseDouble(line.trim()) / 1000.0;
                }
            }
        } catch (Exception e) {
            System.out.println("[Temp] Error leyendo temperatura en Linux: " + e.getMessage());
        }
        return 0.0;
    }

    // -----------------------------------------------------------------------
    // Main: punto de entrada para ejecución standalone (sin script Python)
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        int    numRuns   = 1;
        long   seed      = 1;
        double tempMaxima = 80.0;

        String problema = "OneMax";

        if (args.length > 0) {
            try { numRuns = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { System.err.println("num_runs invalido, usando 1"); }
        }
        if (args.length > 1) {
            try { seed = Long.parseLong(args[1]); }
            catch (NumberFormatException e) { System.err.println("seed invalida, usando 1"); }
        }
        if (args.length > 2) {
            try { tempMaxima = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) { System.err.println("temperatura maxima invalida, usando 80.0"); }
        }
        if (args.length > 5) {
            problema = args[5];
        }

        try {
            new EjecutorAE().ejecutarAlgoritmo(numRuns, seed, problema, tempMaxima);
        } catch (FileNotFoundException e) {
            System.err.println("No se encontro el archivo de configuracion");
            e.printStackTrace();
        } catch (JsonException e) {
            System.err.println("Error en el JSON de configuracion");
            e.printStackTrace();
        }
    }

    /**
     * Carga un problema definido por el usuario desde user-problem.json.
     * El fichero debe tener el formato:
     *   { "clase": "paquete.NombreClase", "parametros": [param1, param2, ...] }
     *
     * Los parametros se pasan como tipos primitivos (int, double) o String.
     * Si el fichero no existe o hay un error, lanza RuntimeException.
     */
    @SuppressWarnings("unchecked")
    private ObjectiveFunction cargarProblemaUsuario() {
        String rutaFichero = System.getProperty("user.problem.file", "user-problem.json");
        try {
            String json = new String(Files.readAllBytes(Paths.get(rutaFichero)));
            JsonObject obj = (JsonObject) Jsoner.deserialize(json);

            String nombreClase = (String) obj.get("clase");
            JsonArray params   = (JsonArray) obj.get("parametros");

            if (nombreClase == null) {
                throw new RuntimeException("El fichero user-problem.json debe tener el campo \"clase\".");
            }

            Class<?> clazz = Class.forName(nombreClase);

            if (params == null || params.isEmpty()) {
                return (ObjectiveFunction) clazz.getDeclaredConstructor().newInstance();
            }

            Object[] args   = new Object[params.size()];
            Class<?>[] tipos = new Class<?>[params.size()];

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long || p instanceof Integer) {
                    args[i]  = ((Number) p).intValue();
                    tipos[i] = int.class;
                } else if (p instanceof Double || p instanceof Float) {
                    args[i]  = ((Number) p).doubleValue();
                    tipos[i] = double.class;
                } else {
                    // String u otro tipo: lo pasamos tal cual
                    args[i]  = p.toString();
                    tipos[i] = String.class;
                }
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor(tipos);
            return (ObjectiveFunction) constructor.newInstance(args);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Clase no encontrada: " + e.getMessage() +
                "\nAsegurate de que el .jar o .class del problema esta en el classpath.", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No se encontro un constructor con esos parametros: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el problema de usuario: " + e.getMessage(), e);
        }
    }

}