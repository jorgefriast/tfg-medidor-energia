package tfgmain;

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class VentanaGraficas extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color OLIVE        = new Color(0x8C, 0xB3, 0x69);
    private static final Color GOLD         = new Color(0xF4, 0xE2, 0x85);
    private static final Color SANDY        = new Color(0xF4, 0xA2, 0x59);
    private static final Color TEAL         = new Color(0x5B, 0x8E, 0x7D);
    private static final Color BRICK        = new Color(0xBC, 0x4B, 0x51);

    // Fondos y textos
    private static final Color BG_MAIN      = new Color(0xFA, 0xF7, 0xF0);
    private static final Color BG_CHART     = new Color(0xFF, 0xFF, 0xFF);
    private static final Color GRID_CLR     = new Color(0xD8, 0xD2, 0xC2);
    private static final Color BG_TAB_IDLE  = new Color(0xE8, 0xE3, 0xD5); 

    private static final Color TEXT_DARK    = new Color(0x2C, 0x2C, 0x24);
    private static final Color TEXT_MID     = new Color(0x6A, 0x64, 0x52);

    public VentanaGraficas() {
        setTitle("Resultados de medicion");
        setSize(1120, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        getContentPane().setBackground(BG_MAIN);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(BG_MAIN);
        tabs.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        tabs.setUI(new RoundedTabbedPaneUI());

        List<Map<String, String>> filas = leerCodeCarbon("codecarbon_final.csv");

        if (filas.isEmpty()) {
            JLabel aviso = new JLabel("No hay datos en codecarbon_final.csv todavia.", SwingConstants.CENTER);
            aviso.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            aviso.setForeground(TEXT_MID);
            add(aviso);
            return;
        }

        tabs.addTab("Emisiones CO2",         crearPanelEmisiones(filas));
        tabs.addTab("Energia CPU vs RAM",    crearPanelEnergia(filas));
        tabs.addTab("Temperatura",           crearPanelTemperatura(filas));

        JPanel panelIntel = crearPanelIntel("intel_logs");
        if (panelIntel != null) tabs.addTab("Potencia Intel (W)", panelIntel);

        JPanel panelEnergiaPorProblema = crearPanelEnergiaPorProblema(filas);
        if (panelEnergiaPorProblema != null) tabs.addTab("Energia por problema", panelEnergiaPorProblema);

        tabs.addTab("Energia por ejecución",     crearPanelEnergiaAcumulada(filas));
        tabs.addTab("Tasa de emisiones",     crearPanelTasaEmisiones(filas));

        JPanel panelDesglose = crearPanelDesglosePorProblema(filas);
        if (panelDesglose != null) tabs.addTab("CPU/RAM/GPU por problema", panelDesglose);

        JPanel panelPotencia = crearPanelPotenciaPorProblema(filas);
        if (panelPotencia != null) tabs.addTab("Potencia por problema", panelPotencia);

        JPanel panelEnergiaVsDescanso = crearPanelEnergiaVsDescanso(filas);
        if (panelEnergiaVsDescanso != null) tabs.addTab("Energia vs Descanso", panelEnergiaVsDescanso);

        add(tabs);
    }

    private void temaGrafico(JFreeChart chart) {
        chart.setBackgroundPaint(BG_CHART);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));
        chart.getTitle().setPaint(TEXT_DARK);
        chart.getTitle().setMargin(10, 0, 10, 0); // Un poco de aire al título
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(BG_CHART);
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
            chart.getLegend().setItemPaint(TEXT_DARK);
            chart.getLegend().setMargin(10, 10, 10, 10);
        }
    }

    private void temaPlotBarras(CategoryPlot plot) {
        temaPlotBarras(plot, false);
    }

    private void temaPlotBarras(CategoryPlot plot, boolean etiquetasLargas) {
        plot.setBackgroundPaint(BG_CHART);
        plot.setRangeGridlinePaint(GRID_CLR);
        plot.setDomainGridlinePaint(GRID_CLR);

        CategoryAxis domainAxis = (CategoryAxis) plot.getDomainAxis();

        if (etiquetasLargas) {
            domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 8));
            domainAxis.setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4.0)
            );
            domainAxis.setMaximumCategoryLabelLines(2);
        } else {
            domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        }

        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        domainAxis.setTickLabelPaint(TEXT_DARK);
        plot.getRangeAxis().setTickLabelPaint(TEXT_DARK);
        domainAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        domainAxis.setLabelPaint(TEXT_DARK);
        plot.getRangeAxis().setLabelPaint(TEXT_DARK);
        plot.setOutlinePaint(GRID_CLR);
    }

    private void temaPlotXY(XYPlot plot) {
        plot.setBackgroundPaint(BG_CHART);
        plot.setDomainGridlinePaint(GRID_CLR);
        plot.setRangeGridlinePaint(GRID_CLR);
        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        plot.getDomainAxis().setTickLabelPaint(TEXT_DARK);
        plot.getRangeAxis().setTickLabelPaint(TEXT_DARK);
        plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        plot.getDomainAxis().setLabelPaint(TEXT_DARK);
        plot.getRangeAxis().setLabelPaint(TEXT_DARK);
        plot.setOutlinePaint(GRID_CLR);
    }

    private JPanel wrapChart(JFreeChart chart) {
        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(BG_CHART);
        cp.setDoubleBuffered(false); 
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CHART);
        p.add(cp, BorderLayout.CENTER);
        
        p.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 10, 10, 10),
            BorderFactory.createLineBorder(GRID_CLR, 1)
        ));
        return p;
    }

    // Pestaña 1: Emisiones CO2 
    private JPanel crearPanelEmisiones(List<Map<String, String>> filas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int i = 1;
        for (Map<String, String> fila : filas) {
            double emisiones = parseDouble(fila.get("emissions")) * 1e6;
            String problema  = fila.getOrDefault("problema", "");
            String label     = problema.isEmpty() ? "Ejec. " + i : problema + " (" + i + ")";
            dataset.addValue(emisiones, "ug CO2", label);
            i++;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Emisiones de CO2 por ejecucion",
                "Ejecucion", "Emisiones (ug CO2eq)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot, true);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, TEAL);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 2: Energía 
    private JPanel crearPanelEnergia(List<Map<String, String>> filas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int i = 1;
        for (Map<String, String> fila : filas) {
            double cpu = parseDouble(fila.get("cpu_energy")) * 1e6;
            double ram = parseDouble(fila.get("ram_energy")) * 1e6;
            String problema = fila.getOrDefault("problema", "");
            String label    = problema.isEmpty() ? "Ejec. " + i : problema + " (" + i + ")";
            dataset.addValue(cpu, "CPU", label);
            dataset.addValue(ram, "RAM", label);
            i++;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Energia consumida: CPU vs RAM",
                "Ejecucion", "Energia (uWh)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot, true);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, OLIVE);
        r.setSeriesPaint(1, SANDY);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 3: Temperatura media
    private JPanel crearPanelTemperatura(List<Map<String, String>> filas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int i = 1;
        for (Map<String, String> fila : filas) {
            double temp     = parseDouble(fila.get("temperatura"));
            String problema = fila.getOrDefault("problema", "");
            String descanso = fila.getOrDefault("descanso_seg", "");
            String sufDescanso = descanso.isEmpty() ? "" : " [" + descanso + "s]";
            String label    = problema.isEmpty() ? "Ejec. " + i + sufDescanso : problema + " (" + i + ")" + sufDescanso;
            dataset.addValue(temp, "Temp. media (C)", label);
            i++;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Temperatura media del procesador por ejecucion",
                "Ejecucion [descanso previo]", "Temperatura (C)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                "El valor entre corchetes indica los segundos de descanso antes de esa ejecucion.",
                new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 10)));

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot, true);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, BRICK);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 4: Potencia Intel 
    private JPanel crearPanelIntel(String carpeta) {
        File dir = new File("monitor_logs");
        if (!dir.exists() || dir.listFiles() == null) return null;

        File[] jsons = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsons == null || jsons.length == 0) return null;
        Arrays.sort(jsons);

        XYSeriesCollection dataset = new XYSeriesCollection();
        Color[] lineColors = {TEAL, OLIVE, SANDY, BRICK, GOLD, 
                              new Color(0x3E, 0x6E, 0x5D), new Color(0xD4, 0x82, 0x39)};

        for (File jsonFile : jsons) {
            String nombre = jsonFile.getName().replace("monitor_log_", "").replace(".json", "");
            XYSeries serie = new XYSeries(nombre);

            try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) sb.append(linea);
                String json = sb.toString().trim();

                json = json.replaceAll("^\\[|\\]$", "");
                String[] entradas = json.split("\\},\\s*\\{");
                for (String entrada : entradas) {
                    entrada = entrada.replaceAll("[{}]", "");
                    double tiempo   = Double.NaN;
                    double potencia = Double.NaN;
                    for (String par : entrada.split(",")) {
                        String[] kv = par.split(":");
                        if (kv.length < 2) continue;
                        String clave = kv[0].trim().replace("\"", "");
                        String valor = kv[1].trim().replace("\"", "");
                        if (valor.equals("null")) continue;
                        try {
                            if (clave.equals("tiempo"))   tiempo   = Double.parseDouble(valor);
                            if (clave.equals("potencia")) potencia = Double.parseDouble(valor);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (!Double.isNaN(tiempo) && !Double.isNaN(potencia)) {
                        serie.add(tiempo, potencia);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (serie.getItemCount() > 0) dataset.addSeries(serie);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Potencia del procesador durante la ejecucion",
                "Tiempo (s)", "Potencia (W)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        temaGrafico(chart);
        XYPlot plot = chart.getXYPlot();
        temaPlotXY(plot);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, lineColors[i % lineColors.length]);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
        }
        renderer.setDefaultShapesVisible(false);
        plot.setRenderer(renderer);

        return wrapChart(chart);
    }

    // Pestaña 5: Energía media por problema 
    private JPanel crearPanelEnergiaPorProblema(List<Map<String, String>> filas) {
        Map<String, List<Double>> porProblema = new LinkedHashMap<>();
        for (Map<String, String> fila : filas) {
            String problema = fila.getOrDefault("problema", "");
            if (problema.isEmpty()) continue;
            double energia = parseDouble(fila.get("energy_consumed")) * 1000.0;
            porProblema.computeIfAbsent(problema, k -> new ArrayList<>()).add(energia);
        }
        if (porProblema.isEmpty()) return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, List<Double>> entry : porProblema.entrySet()) {
            double media = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            dataset.addValue(media, "Energia media (Wh)", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Energia media consumida por tipo de problema",
                "Problema", "Energia media (Wh)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, OLIVE);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 6: Energía por ejecución (barras)
    private JPanel crearPanelEnergiaAcumulada(List<Map<String, String>> filas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int i = 1;
        for (Map<String, String> fila : filas) {
            double energia  = parseDouble(fila.get("energy_consumed")) * 1000.0;
            String problema = fila.getOrDefault("problema", "");
            String descanso = fila.getOrDefault("descanso_seg", "");
            String sufDescanso = descanso.isEmpty() ? "" : " [" + descanso + "s]";
            String label    = problema.isEmpty() ? "Ejec. " + i + sufDescanso : problema + " (" + i + ")" + sufDescanso;
            dataset.addValue(energia, "Wh", label);
            i++;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Energia consumida por ejecucion",
                "Ejecucion [descanso previo]", "Energia (Wh)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                "Fuente: CodeCarbon (CPU + RAM + GPU). El valor entre corchetes indica los segundos de descanso antes de esa ejecucion.",
                new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 10)));

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot, true);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, SANDY);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 7: Tasa de emisiones
    private JPanel crearPanelTasaEmisiones(List<Map<String, String>> filas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int i = 1;
        for (Map<String, String> fila : filas) {
            double tasa     = parseDouble(fila.get("emissions_rate")) * 1000.0;
            String problema = fila.getOrDefault("problema", "");
            String label    = problema.isEmpty() ? "Ejec. " + i : problema + " (" + i + ")";
            dataset.addValue(tasa, "g CO2/kWh", label);
            i++;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Tasa de emisiones por ejecucion",
                "Ejecucion", "Tasa (g CO2/kWh)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot, true);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, BRICK);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 8: Desglose CPU/RAM/GPU por problema
    private JPanel crearPanelDesglosePorProblema(List<Map<String, String>> filas) {
        Map<String, List<double[]>> porProblema = new LinkedHashMap<>();
        for (Map<String, String> fila : filas) {
            String problema = fila.getOrDefault("problema", "");
            if (problema.isEmpty()) continue;
            double cpu = parseDouble(fila.get("cpu_energy")) * 1e6;
            double ram = parseDouble(fila.get("ram_energy")) * 1e6;
            double gpu = parseDouble(fila.get("gpu_energy")) * 1e6;
            porProblema.computeIfAbsent(problema, k -> new ArrayList<>()).add(new double[]{cpu, ram, gpu});
        }
        if (porProblema.isEmpty()) return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, List<double[]>> entry : porProblema.entrySet()) {
            double mediaCpu = entry.getValue().stream().mapToDouble(v -> v[0]).average().orElse(0.0);
            double mediaRam = entry.getValue().stream().mapToDouble(v -> v[1]).average().orElse(0.0);
            double mediaGpu = entry.getValue().stream().mapToDouble(v -> v[2]).average().orElse(0.0);
            dataset.addValue(mediaCpu, "CPU", entry.getKey());
            dataset.addValue(mediaRam, "RAM", entry.getKey());
            dataset.addValue(mediaGpu, "GPU", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Desglose energetico medio por componente y problema",
                "Problema", "Energia media (uWh)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, OLIVE);     // CPU
        r.setSeriesPaint(1, TEAL);      // RAM
        r.setSeriesPaint(2, SANDY);     // GPU
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 9: Potencia media por problema 
    private JPanel crearPanelPotenciaPorProblema(List<Map<String, String>> filas) {
        Map<String, List<Double>> porProblema = new LinkedHashMap<>();
        for (Map<String, String> fila : filas) {
            String problema = fila.getOrDefault("problema", "");
            if (problema.isEmpty()) continue;
            double potencia = parseDouble(fila.get("potencia_media"));
            porProblema.computeIfAbsent(problema, k -> new ArrayList<>()).add(potencia);
        }
        if (porProblema.isEmpty()) return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, List<Double>> entry : porProblema.entrySet()) {
            double media = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            dataset.addValue(media, "Potencia media (W)", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Potencia media del procesador por tipo de problema",
                "Problema", "Potencia media (W)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, TEAL);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    // Pestaña 10: Energia media vs tiempo de descanso 
    private JPanel crearPanelEnergiaVsDescanso(List<Map<String, String>> filas) {
        // Agrupar energía media por tiempo de descanso
        Map<Integer, List<Double>> porDescanso = new TreeMap<>();
        for (Map<String, String> fila : filas) {
            String descansoStr = fila.getOrDefault("descanso_seg", "").trim();
            if (descansoStr.isEmpty()) continue;
            try {
                int descanso = Integer.parseInt(descansoStr);
                double energia = parseDouble(fila.get("energy_consumed")) * 1000.0;
                porDescanso.computeIfAbsent(descanso, k -> new ArrayList<>()).add(energia);
            } catch (NumberFormatException ignored) {}
        }
        if (porDescanso.size() < 2) return null; // Necesita al menos 2 grupos distintos para ser útil

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<Integer, List<Double>> entry : porDescanso.entrySet()) {
            double media = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            dataset.addValue(media, "Energia media (Wh)", entry.getKey() + "s");
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Energia media consumida segun descanso previo",
                "Descanso entre ejecuciones (s)", "Energia media (Wh)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                "Un mayor descanso permite al procesador enfriarse, reduciendo el consumo por efecto histeresis.",
                new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 10)));

        temaGrafico(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        temaPlotBarras(plot);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setSeriesPaint(0, GOLD);
        r.setShadowVisible(false);
        r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());

        return wrapChart(chart);
    }

    private List<Map<String, String>> leerCodeCarbon(String rutaCSV) {
        List<Map<String, String>> filas = new ArrayList<>();
        File f = new File(rutaCSV);
        if (!f.exists()) return filas;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String cabecera = br.readLine();
            if (cabecera == null) return filas;
            String[] columnas = cabecera.split(",");

            String linea;
            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",");
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < columnas.length && i < valores.length; i++) {
                    fila.put(columnas[i].trim(), valores[i].trim());
                }
                filas.add(fila);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filas;
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private class RoundedTabbedPaneUI extends BasicTabbedPaneUI {
        private static final int CORNER_RADIUS = 16;

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets.left = 12;
            tabAreaInsets.top = 10;
            tabAreaInsets.bottom = 10;
            tabInsets = new Insets(8, 16, 8, 16);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected) {
                g2.setColor(TEAL); 
            } else {
                g2.setColor(BG_TAB_IDLE); 
            }
            
            g2.fillRoundRect(x, y + 2, w - 4, h - 4, CORNER_RADIUS, CORNER_RADIUS);
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);

            if (isSelected) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(TEXT_DARK);
            }

            FontMetrics fm = g2.getFontMetrics();
            int tx = textRect.x + (textRect.width - fm.stringWidth(title)) / 2;
            int ty = textRect.y + fm.getAscent() + (textRect.height - fm.getHeight()) / 2 + 1;
            
            g2.drawString(title, tx, ty);
            g2.dispose();
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            int width = tabPane.getWidth();
            int height = tabPane.getHeight();
            Insets insets = tabPane.getInsets();

            int x = insets.left;
            int y = insets.top;
            int w = width - insets.right - insets.left;
            
            y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(TEAL);
            g2.drawLine(x, y, x + w, y);
            g2.dispose();
        }
    }
}