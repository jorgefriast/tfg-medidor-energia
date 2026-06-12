package problema;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import es.uma.lcc.caesium.ea.base.Genotype;
import es.uma.lcc.caesium.ea.base.Individual;
import es.uma.lcc.caesium.ea.fitness.DiscreteObjectiveFunction;
import es.uma.lcc.caesium.ea.fitness.OptimizationSense;

/**
 * Problema de la Mochila 0/1 (Knapsack Problem).
 *
 * Cada variable del genotipo representa si un objeto se mete (1) o no (0)
 * en la mochila. El objetivo es maximizar el valor total sin superar
 * la capacidad máxima.
 *
 * Formato del fichero de datos:
 *   Línea 1: numObjetos capacidad
 *   Líneas siguientes (una por objeto): peso valor
 *
 * Ejemplo (mochila.dat):
 *   5 10
 *   2 6
 *   3 10
 *   4 12
 *   5 13
 *   1 3
 */
public class Mochila extends DiscreteObjectiveFunction {

    /** Capacidad máxima de la mochila */
    private int capacidad;

    /** Peso de cada objeto */
    private int[] pesos;

    /** Valor de cada objeto */
    private int[] valores;

    /**
     * Constructor que lee los datos del problema desde un fichero.
     * @param fichero ruta al fichero de datos
     */
    public Mochila(String fichero) {
        // Llamamos a super con 1 variable provisionalmente; se actualizará al leer el fichero
        super(1, 2);
        cargarDatos(fichero);
    }

    /**
     * Lee el fichero de datos y carga los pesos, valores y capacidad.
     * También actualiza numvars con el número real de objetos.
     */
    private void cargarDatos(String fichero) {
        try (Scanner sc = new Scanner(new File(fichero))) {
            int numObjetos = sc.nextInt();
            capacidad = sc.nextInt();

            pesos  = new int[numObjetos];
            valores = new int[numObjetos];

            for (int i = 0; i < numObjetos; i++) {
                pesos[i]  = sc.nextInt();
                valores[i] = sc.nextInt();
            }

            // Actualizamos el número de variables al número real de objetos
            numvars = numObjetos;
            numval  = new int[numObjetos];
            for (int i = 0; i < numObjetos; i++) {
                numval[i] = 2; // binario: 0 o 1
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException("No se encontró el fichero de datos de la mochila: " + fichero, e);
        }
    }

    @Override
    public OptimizationSense getOptimizationSense() {
        return OptimizationSense.MAXIMIZATION;
    }

    /**
     * Evalúa una solución.
     * Si el peso total supera la capacidad, el fitness es 0 (solución inválida).
     * Si no, el fitness es la suma de valores de los objetos seleccionados.
     */
    @Override
    protected double _evaluate(Individual ind) {
        Genotype g = ind.getGenome();

        int pesoTotal  = 0;
        int valorTotal = 0;

        for (int i = 0; i < numvars; i++) {
            int bit = (Integer) g.getGene(i);
            pesoTotal  += bit * pesos[i];
            valorTotal += bit * valores[i];
        }

        // Penalización: si no cabe en la mochila, fitness = 0
        if (pesoTotal > capacidad) {
            return 0.0;
        }

        return (double) valorTotal;
    }
}