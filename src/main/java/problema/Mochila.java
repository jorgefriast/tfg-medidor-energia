package problema;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import es.uma.lcc.caesium.ea.base.Genotype;
import es.uma.lcc.caesium.ea.base.Individual;
import es.uma.lcc.caesium.ea.fitness.DiscreteObjectiveFunction;
import es.uma.lcc.caesium.ea.fitness.OptimizationSense;


public class Mochila extends DiscreteObjectiveFunction {

    private int capacidad;

    private int[] pesos;

    private int[] valores;

    public Mochila(String fichero) {
        // Llamamos a super con 1 variable provisionalmente; se actualizará al leer el fichero
        super(1, 2);
        cargarDatos(fichero);
    }

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