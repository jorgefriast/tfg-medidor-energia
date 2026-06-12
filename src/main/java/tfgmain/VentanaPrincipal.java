package tfgmain;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;

import medicion.MedidorEnergia;

public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    
    private static final Color OLIVE        = new Color(0x8C, 0xB3, 0x69);   // #8CB369
    private static final Color GOLD         = new Color(0xF4, 0xE2, 0x85);   // #F4E285
    private static final Color SANDY        = new Color(0xF4, 0xA2, 0x59);   // #F4A259
    private static final Color TEAL         = new Color(0x5B, 0x8E, 0x7D);   // #5B8E7D
    private static final Color BRICK        = new Color(0xBC, 0x4B, 0x51);   // #BC4B51

    // Variantes hover / dark
    private static final Color OLIVE_DARK   = new Color(0x6E, 0x8F, 0x4F);
    private static final Color SANDY_DARK   = new Color(0xD4, 0x82, 0x39);
    private static final Color TEAL_DARK    = new Color(0x3E, 0x6E, 0x5D);
    private static final Color BRICK_DARK   = new Color(0x96, 0x2E, 0x33);

    // Fondos y textos
    private static final Color BG_MAIN      = new Color(0xFA, 0xF7, 0xF0);   
    private static final Color BG_PANEL     = new Color(0xFF, 0xFF, 0xFF);
    private static final Color BG_CONSOLE   = new Color(0x1E, 0x2A, 0x24);   
    private static final Color BG_INPUT     = new Color(0xF0, 0xED, 0xE4);
    private static final Color BORDER_COLOR = new Color(0xD8, 0xD2, 0xC2);

    private static final Color TEXT_DARK    = new Color(0x2C, 0x2C, 0x24);
    private static final Color TEXT_MID     = new Color(0x6A, 0x64, 0x52);
    private static final Color TEXT_LIGHT   = new Color(0xA8, 0xA2, 0x90);
    private static final Color CONSOLE_TEXT = new Color(0xA8, 0xD8, 0xB0);   

    // Fuentes
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_CONSOLE = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_STATUS  = new Font("Segoe UI", Font.ITALIC, 11);

    // Componentes
    private JPanel panelGeneral;
    private JTextArea consola;
    private MedidorEnergia medidor;
    private JLabel lblContador;
    private JLabel lblSeed;
    private JSpinner spinnerNumRuns;
    private JSpinner spinnerNumMediciones;
    private JSpinner spinnerDescanso;
    private JSpinner spinnerTempMaxima;
    private JComboBox<String> comboProblema;
    private JSpinner spinnerPotenciaMax;
    private JSpinner spinnerTiempoMax;
    private int numeroEjecuciones = 0;
    private int seedActual = 1;
    private JCheckBox chkTempMaxima;
    private JCheckBox chkPotenciaMax;
    private JCheckBox chkTiempoMax;
    private JSpinner spinnerEnergiaMax;
    private JCheckBox chkEnergiaMax;
    private JLabel status;
    private JLabel lblUserProblemFile;
    private JPanel panelUserProblem;
    private JTextField txtNombreProblema;
    private JTextField txtFicheroDatos;

    public VentanaPrincipal() {
        setTitle("EnergyMeter — Medidor de Consumo Energetico");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                File logSuelto = new File("intel_power_gadget_log.csv");
                if (logSuelto.exists()) logSuelto.delete();
                dispose();
                System.exit(0);
            }
        });

        setSize(1000, 680);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);

        panelGeneral = new JPanel(new BorderLayout(0, 0));
        panelGeneral.setBackground(BG_MAIN);
        setContentPane(panelGeneral);

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(TEAL);
        header.setBorder(new EmptyBorder(14, 22, 14, 22));

        // Título
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tituloPanel.setOpaque(false);

        JPanel accentBar = new JPanel();
        accentBar.setBackground(SANDY);
        accentBar.setPreferredSize(new Dimension(4, 24));
        accentBar.setOpaque(true);

        JLabel titulo = new JLabel("Medicion de consumo energetico");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(Color.WHITE);

        tituloPanel.add(accentBar);
        tituloPanel.add(Box.createHorizontalStrut(4));
        tituloPanel.add(titulo);
        header.add(tituloPanel, BorderLayout.WEST);

        // Botones de acción
        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botonesPanel.setOpaque(false);

        JButton btnMedir    = crearBotonSolido("Iniciar medicion",  OLIVE,  Color.WHITE);
        JButton btnReset    = crearBotonSolido("Limpiar datos",      BRICK,  Color.WHITE);
        JButton btnGraficas = crearBotonSolido("Ver estadisticas",   SANDY,  Color.WHITE);

        botonesPanel.add(btnMedir);
        botonesPanel.add(btnReset);
        botonesPanel.add(btnGraficas);
        header.add(botonesPanel, BorderLayout.EAST);
        
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBackground(BG_MAIN);
        configPanel.setBorder(new EmptyBorder(10, 14, 6, 14));

        JPanel filaParametros = crearPanelSeccion("PARAMETROS", OLIVE);

        JLabel lblNumRuns = crearLabel("Runs:");
        spinnerNumRuns = crearSpinner(1, 1, 1000, 1, 68);

        JLabel lblNumMediciones = crearLabel("Mediciones:");
        spinnerNumMediciones = crearSpinner(1, 1, 1000, 1, 68);

        JLabel lblDescanso = crearLabel("Descanso (s):");
        spinnerDescanso = crearSpinner(0, 0, 3600, 1, 68);

        JLabel lblProblema = crearLabel("Problema:");
        comboProblema = new JComboBox<>(new String[]{"OneMax", "LeadingOnes", "DeceptiveTrap", "MMDP", "UserProblem"});
        estilizarCombo(comboProblema);

        filaParametros.add(lblNumRuns);
        filaParametros.add(spinnerNumRuns);
        filaParametros.add(separador());
        filaParametros.add(lblNumMediciones);
        filaParametros.add(spinnerNumMediciones);
        filaParametros.add(separador());
        filaParametros.add(lblDescanso);
        filaParametros.add(spinnerDescanso);
        filaParametros.add(separador());
        filaParametros.add(lblProblema);
        filaParametros.add(comboProblema);

        // Panel "User Problem" — aparece solo cuando se selecciona UserProblem
        panelUserProblem = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panelUserProblem.setOpaque(false);

        JLabel lblNombreProblema = crearLabel("Clase:");
        txtNombreProblema = new JTextField("Mochila", 10);
        txtNombreProblema.setFont(FONT_LABEL);
        txtNombreProblema.setBackground(BG_INPUT);
        txtNombreProblema.setForeground(TEXT_DARK);
        txtNombreProblema.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(2, 6, 2, 6)
        ));
        txtNombreProblema.setPreferredSize(new java.awt.Dimension(100, 28));

        JLabel lblFicheroDatos = crearLabel("Datos:");
        txtFicheroDatos = new JTextField("mochila.dat", 12);
        txtFicheroDatos.setFont(FONT_LABEL);
        txtFicheroDatos.setBackground(BG_INPUT);
        txtFicheroDatos.setForeground(TEXT_DARK);
        txtFicheroDatos.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(2, 6, 2, 6)
        ));
        txtFicheroDatos.setPreferredSize(new java.awt.Dimension(110, 28));

        // Guardamos referencias para usarlas al lanzar la medición
        lblUserProblemFile = new JLabel(""); 
        lblUserProblemFile.setVisible(false);

        // Cada vez que cambia algún campo, regeneramos el user-problem.json automáticamente
        java.awt.event.ActionListener generarJson = ev -> {
            String nombreClase = txtNombreProblema.getText().trim();
            String ficheroDatos = txtFicheroDatos.getText().trim();
            if (!nombreClase.isEmpty()) {
                // Normalizamos: primera letra mayúscula
                nombreClase = Character.toUpperCase(nombreClase.charAt(0)) + nombreClase.substring(1);
                String claseCompleta = "problema." + nombreClase;
                String json = "{\n  \"clase\": \"" + claseCompleta + "\"";
                if (!ficheroDatos.isEmpty()) {
                	json += ",\n  \"parametros\": [\"data/" + ficheroDatos + "\"]";
                }
                
                json += "\n}";
                try {
                    java.nio.file.Files.write(
                        java.nio.file.Paths.get("user-problem.json"),
                        json.getBytes()
                    );
                    lblUserProblemFile.setText(claseCompleta);
                } catch (IOException ioex) {
                    log("[UserProblem] Error generando JSON: " + ioex.getMessage());
                }
            }
        };

        txtNombreProblema.addActionListener(generarJson);
        txtFicheroDatos.addActionListener(generarJson);
        txtNombreProblema.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { generarJson.actionPerformed(null); }
        });
        txtFicheroDatos.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { generarJson.actionPerformed(null); }
        });

        panelUserProblem.add(lblNombreProblema);
        panelUserProblem.add(txtNombreProblema);
        panelUserProblem.add(separador());
        panelUserProblem.add(lblFicheroDatos);
        panelUserProblem.add(txtFicheroDatos);
        panelUserProblem.setVisible(false);

        comboProblema.addActionListener(ev -> {
            boolean esUserProblem = "UserProblem".equals(comboProblema.getSelectedItem());
            panelUserProblem.setVisible(esUserProblem);
            panelGeneral.revalidate();
            panelGeneral.repaint();
        });

        JPanel filaParada = crearPanelSeccion("CONDICIONES DE PARADA", SANDY_DARK);

        chkTempMaxima   = crearCheckbox("Temp. max. (C)");
        spinnerTempMaxima = crearSpinner(80, 30, 120, 1, 68);
        chkTempMaxima.setSelected(true);

        chkPotenciaMax  = crearCheckbox("Potencia max. (W)");
        spinnerPotenciaMax = crearSpinner(50, 1, 200, 1, 68);
        spinnerPotenciaMax.setEnabled(false);

        chkTiempoMax    = crearCheckbox("Tiempo max. (s)");
        spinnerTiempoMax = crearSpinner(60, 1, 3600, 10, 68);
        spinnerTiempoMax.setEnabled(false);

        chkEnergiaMax    = crearCheckbox("Energia max. (Wh)");
        spinnerEnergiaMax = crearSpinner(1, 0, 1000, 1, 80);
        spinnerEnergiaMax.setModel(new SpinnerNumberModel(0.01, 0.0001, 1000.0, 0.001));
        spinnerEnergiaMax.setEnabled(false);
        JSpinner.NumberEditor editorEnergia = new JSpinner.NumberEditor(spinnerEnergiaMax, "0.0000");
        spinnerEnergiaMax.setEditor(editorEnergia);

        chkTempMaxima.addActionListener(e  -> spinnerTempMaxima.setEnabled(chkTempMaxima.isSelected()));
        chkPotenciaMax.addActionListener(e -> spinnerPotenciaMax.setEnabled(chkPotenciaMax.isSelected()));
        chkTiempoMax.addActionListener(e   -> spinnerTiempoMax.setEnabled(chkTiempoMax.isSelected()));
        chkEnergiaMax.addActionListener(e  -> spinnerEnergiaMax.setEnabled(chkEnergiaMax.isSelected()));

        filaParada.add(chkTempMaxima);
        filaParada.add(spinnerTempMaxima);
        filaParada.add(separador());
        filaParada.add(chkPotenciaMax);
        filaParada.add(spinnerPotenciaMax);
        filaParada.add(separador());
        filaParada.add(chkTiempoMax);
        filaParada.add(spinnerTiempoMax);
        filaParada.add(chkEnergiaMax);
        filaParada.add(spinnerEnergiaMax);

        // Estilizar panelUserProblem como una sección igual que las otras filas
        panelUserProblem.setBackground(BG_PANEL);
        panelUserProblem.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createMatteBorder(0, 3, 0, 0, TEAL)
            ),
            new EmptyBorder(2, 10, 2, 10)
        ));
        JLabel lblSeccionUser = new JLabel("PROBLEMA USUARIO");
        lblSeccionUser.setFont(FONT_SECTION);
        lblSeccionUser.setForeground(TEXT_MID);
        lblSeccionUser.setBorder(new EmptyBorder(0, 0, 0, 14));
        panelUserProblem.add(lblSeccionUser, 0);

        configPanel.add(filaParametros);
        configPanel.add(Box.createVerticalStrut(2));
        configPanel.add(filaParada);
        configPanel.add(Box.createVerticalStrut(2));
        configPanel.add(panelUserProblem);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(BG_MAIN);
        topWrapper.add(header, BorderLayout.NORTH);
        topWrapper.add(configPanel, BorderLayout.CENTER);
        panelGeneral.add(topWrapper, BorderLayout.NORTH);

        consola = new JTextArea();
        consola.setEditable(false);
        consola.setFont(FONT_CONSOLE);
        consola.setBackground(BG_CONSOLE);
        consola.setForeground(CONSOLE_TEXT);
        consola.setCaretColor(CONSOLE_TEXT);
        consola.setMargin(new Insets(10, 14, 10, 14));
        consola.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(consola);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.setBackground(BG_CONSOLE);
        scroll.getViewport().setBackground(BG_CONSOLE);

        JPanel consolaHeaderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        consolaHeaderRow.setBackground(BG_MAIN);
        consolaHeaderRow.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel consolaLabel = new JLabel("SALIDA DEL SISTEMA");
        consolaLabel.setFont(FONT_SECTION);
        consolaLabel.setForeground(TEXT_MID);

        JPanel pill = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEAL);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(8, 8));

        consolaHeaderRow.add(pill);
        consolaHeaderRow.add(consolaLabel);

        JPanel consolaWrapper = new JPanel(new BorderLayout(0, 6));
        consolaWrapper.setBackground(BG_MAIN);
        consolaWrapper.setBorder(new EmptyBorder(4, 14, 4, 14));
        consolaWrapper.add(consolaHeaderRow, BorderLayout.NORTH);
        consolaWrapper.add(scroll, BorderLayout.CENTER);
        panelGeneral.add(consolaWrapper, BorderLayout.CENTER);
        
        
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_PANEL);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            new EmptyBorder(7, 18, 7, 18)
        ));

        status = new JLabel("Listo para medir.");
        status.setFont(FONT_STATUS);
        status.setForeground(TEXT_LIGHT);

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightStatus.setOpaque(false);

        lblSeed = new JLabel("Seed actual: 1");
        lblSeed.setFont(FONT_SMALL);
        lblSeed.setForeground(TEXT_LIGHT);

        lblContador = new JLabel("Ejecuciones registradas: 0");
        lblContador.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblContador.setForeground(TEAL);

        JLabel sep = new JLabel("|");
        sep.setForeground(BORDER_COLOR);

        rightStatus.add(lblSeed);
        rightStatus.add(sep);
        rightStatus.add(lblContador);

        statusBar.add(status, BorderLayout.WEST);
        statusBar.add(rightStatus, BorderLayout.EAST);
        panelGeneral.add(statusBar, BorderLayout.SOUTH);

        
        medidor = new MedidorEnergia(this::log);
        contarEjecucionesExistentes();
        seedActual = numeroEjecuciones + 1;
        lblSeed.setText("Seed actual: " + seedActual);

        // Acción botón Iniciar medición
        btnMedir.addActionListener(e -> {
            btnMedir.setEnabled(false);
            btnReset.setEnabled(false);
            spinnerNumRuns.setEnabled(false);
            spinnerNumMediciones.setEnabled(false);
            spinnerDescanso.setEnabled(false);
            spinnerTempMaxima.setEnabled(false);
            comboProblema.setEnabled(false);
            panelUserProblem.setVisible(false);
            spinnerPotenciaMax.setEnabled(false);
            spinnerTiempoMax.setEnabled(false);
            spinnerEnergiaMax.setEnabled(false);
            status.setText("Ejecutando medicion...");
            status.setForeground(SANDY);

            int numRuns          = (Integer) spinnerNumRuns.getValue();
            int numMediciones    = (Integer) spinnerNumMediciones.getValue();
            int descansoSegundos = (Integer) spinnerDescanso.getValue();
            String problema      = (String)  comboProblema.getSelectedItem();

            if ("UserProblem".equals(problema) && !validarUserProblem()) {
                btnMedir.setEnabled(true);
                btnReset.setEnabled(true);
                spinnerNumRuns.setEnabled(true);
                spinnerNumMediciones.setEnabled(true);
                spinnerDescanso.setEnabled(true);
                spinnerTempMaxima.setEnabled(chkTempMaxima.isSelected());
                comboProblema.setEnabled(true);
                spinnerPotenciaMax.setEnabled(chkPotenciaMax.isSelected());
                spinnerTiempoMax.setEnabled(chkTiempoMax.isSelected());
                panelUserProblem.setVisible(true);
                status.setText("Listo para medir.");
                status.setForeground(TEXT_LIGHT);
                return;
            }

            int tempMaxima  = chkTempMaxima.isSelected()  ? (Integer) spinnerTempMaxima.getValue()  : -1;
            int potenciaMax = chkPotenciaMax.isSelected() ? (Integer) spinnerPotenciaMax.getValue() : -1;
            int tiempoMax      = chkTiempoMax.isSelected()   ? (Integer) spinnerTiempoMax.getValue()   : -1;
            double energiaMax  = chkEnergiaMax.isSelected()  ? ((Number) spinnerEnergiaMax.getValue()).doubleValue() : 0.0;
            int seedInicial = seedActual;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    for (int i = 0; i < numMediciones; i++) {
                        int seedEsta = seedInicial + i;
                        int descansoAntes = (i == 0) ? 0 : descansoSegundos;
                        log("----");
                        log("Iniciando medicion " + (numeroEjecuciones + i + 1) + " (seed " + seedEsta +
                            ", problema: " + problema +
                            (tempMaxima  > 0 ? ", temp.max. "    + tempMaxima  + "C" : "") +
                            (potenciaMax > 0 ? ", potencia max. " + potenciaMax + "W"  : "") +
                            (tiempoMax   > 0 ? ", tiempo max. "   + tiempoMax   + "s"  : "") +
                            (energiaMax  > 0 ? ", energia max. "  + energiaMax  + "Wh" : "") + ")...");
                        log("----");
                        medidor.iniciarMedicion(numRuns, seedEsta, problema, tempMaxima, potenciaMax, tiempoMax, energiaMax, descansoAntes);
                        medidor.finalizarMedicion();

                        if (descansoSegundos > 0 && i < numMediciones - 1) {
                            log("[Descanso] Esperando " + descansoSegundos + "s antes de la siguiente medicion...");
                            try {
                                Thread.sleep(descansoSegundos * 1000L);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log("[Descanso] Interrumpido.");
                                break;
                            }
                            log("[Descanso] Listo, arrancando siguiente medicion.");
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    numeroEjecuciones += numMediciones;
                    seedActual += numMediciones;
                    actualizarContador();
                    lblSeed.setText("Seed actual: " + seedActual);
                    log("----");
                    log("Todas las mediciones completadas (" + numMediciones + "). Resultados en:");
                    log("  - codecarbon_final.csv");
                    log("----");
                    status.setText("Completadas " + numMediciones + " mediciones. Total: " + numeroEjecuciones + " ejecuciones.");
                    status.setForeground(OLIVE);
                    btnMedir.setEnabled(true);
                    btnReset.setEnabled(true);
                    spinnerNumRuns.setEnabled(true);
                    spinnerNumMediciones.setEnabled(true);
                    spinnerDescanso.setEnabled(true);
                    chkTempMaxima.setEnabled(true);
                    chkPotenciaMax.setEnabled(true);
                    chkTiempoMax.setEnabled(true);
                    chkEnergiaMax.setEnabled(true);
                    comboProblema.setEnabled(true);
                    if ("UserProblem".equals(comboProblema.getSelectedItem())) {
                        panelUserProblem.setVisible(true);
                    }
                    spinnerTempMaxima.setEnabled(chkTempMaxima.isSelected());
                    spinnerPotenciaMax.setEnabled(chkPotenciaMax.isSelected());
                    spinnerTiempoMax.setEnabled(chkTiempoMax.isSelected());
                    spinnerEnergiaMax.setEnabled(chkEnergiaMax.isSelected());
                }
            };
            worker.execute();
        });

        // Acción botón Ver estadísticas
        btnGraficas.addActionListener(e -> {
            File csvFile = new File("codecarbon_final.csv");
            if (!csvFile.exists()) {
                mostrarDialogo("Sin datos", "Todavia no hay datos.\nRealiza al menos una medicion primero.",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            new VentanaGraficas().setVisible(true);
        });

        // Acción botón Limpiar datos
        btnReset.addActionListener(e -> {
        	int confirmacion = JOptionPane.showConfirmDialog(
        		    this,
        		    "¿Seguro que quieres eliminar todos los datos?\n" +
        		    "Esto eliminará:\n" +
        		    "  - codecarbon_final.csv\n" +
        		    "  - carpetas intel_logs y monitor_logs\n\n" +
        		    "Esta acción no se puede deshacer.",
        		    "Confirmar limpieza de datos",
        		    JOptionPane.YES_NO_OPTION,
        		    JOptionPane.WARNING_MESSAGE
        		);
            if (confirmacion == JOptionPane.YES_OPTION) {
                limpiarDatos();
                numeroEjecuciones = 0;
                seedActual = 1;
                actualizarContador();
                lblSeed.setText("Seed actual: 1");
                consola.setText("");
                log("Datos limpiados. Sistema listo para nuevas mediciones.");
                status.setText("Datos eliminados. Listo para medir.");
                status.setForeground(TEXT_LIGHT);
            }
        });
    }


    private JButton crearBotonSolido(String texto, Color fondo, Color textoColor) {
        Color hover = fondo.darker();
        JButton btn = new JButton(texto) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hovered ? hover : fondo;
                if (!isEnabled()) bg = new Color(0xC8, 0xC4, 0xB4);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(textoColor);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        return btn;
    }

    private JPanel crearPanelSeccion(String tituloSeccion, Color accentColor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createMatteBorder(0, 3, 0, 0, TEAL)
            ),
            new EmptyBorder(2, 10, 2, 10)
        ));

        JLabel lbl = new JLabel(tituloSeccion);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_MID);
        lbl.setBorder(new EmptyBorder(0, 0, 0, 14));
        panel.add(lbl);
        return panel;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    private JSpinner crearSpinner(int val, int min, int max, int step, int width) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, step));
        sp.setPreferredSize(new Dimension(width, 28));
        sp.setBackground(BG_INPUT);
        sp.setForeground(TEXT_DARK);
        JComponent editor = sp.getEditor();
        editor.setBackground(BG_INPUT);
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(BG_INPUT);
            tf.setForeground(TEXT_DARK);
            tf.setFont(FONT_LABEL);
            tf.setCaretColor(TEXT_DARK);
            tf.setBorder(new EmptyBorder(2, 6, 2, 2));
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setOpaque(true);
        }
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        return sp;
    }

    private JCheckBox crearCheckbox(String texto) {
        JCheckBox chk = new JCheckBox(texto);
        chk.setFont(FONT_LABEL);
        chk.setForeground(TEXT_DARK);
        chk.setBackground(BG_PANEL);
        chk.setOpaque(false);
        chk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return chk;
    }

    private void estilizarCombo(JComboBox<String> combo) {
        combo.setFont(FONT_LABEL);
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_DARK);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        combo.setPreferredSize(new Dimension(150, 28));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? TEAL : BG_INPUT);
                setForeground(isSelected ? Color.WHITE : TEXT_DARK);
                setBorder(new EmptyBorder(3, 8, 3, 8));
                return this;
            }
        });
    }

    private Component separador() {
        return Box.createHorizontalStrut(8);
    }

    private void mostrarDialogo(String titulo, String mensaje, int tipo) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipo);
    }


    private void contarEjecucionesExistentes() {
        File csvFile = new File("codecarbon_final.csv");
        if (csvFile.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
                numeroEjecuciones = -1;
                while (br.readLine() != null) numeroEjecuciones++;
                if (numeroEjecuciones < 0) numeroEjecuciones = 0;
                actualizarContador();
                log("Se encontraron " + numeroEjecuciones + " ejecuciones previas.");
            } catch (Exception e) {
                numeroEjecuciones = 0;
            }
        }
    }

    private void actualizarContador() {
        lblContador.setText("Ejecuciones registradas: " + numeroEjecuciones);
    }

    private void limpiarDatos() {
        String[] archivos = {"codecarbon_final.csv", "intel_power_gadget_log.csv"};
        for (String nombre : archivos) {
            File f = new File(nombre);
            if (f.exists()) {
                if (f.delete()) log("Eliminado: " + nombre);
                else            log("No se pudo eliminar: " + nombre);
            }
        }
        File carpetaMonitor = new File("monitor_logs");
        if (carpetaMonitor.exists()) {
            for (File f : carpetaMonitor.listFiles()) {
                if (f.delete()) log("Eliminado: monitor_logs/" + f.getName());
            }
            log("Carpeta monitor_logs/ vaciada.");
        }
        File carpetaLogs = new File("intel_logs");
        if (carpetaLogs.exists() && carpetaLogs.isDirectory()) {
            File[] logs = carpetaLogs.listFiles();
            if (logs != null) {
                for (File f : logs) {
                    if (f.isFile()) {
                        if (f.delete()) log("Eliminado: intel_logs/" + f.getName());
                        else            log("No se pudo eliminar: intel_logs/" + f.getName());
                    }
                }
            }
            log("Carpeta intel_logs/ vaciada.");
        }
    }

    public void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            consola.append(mensaje + "\n");
            consola.setCaretPosition(consola.getDocument().getLength());
        });
    }


    private boolean validarUserProblem() {
        boolean ok = true;

        // Resetear estilos
        txtNombreProblema.setBackground(BG_INPUT);
        txtFicheroDatos.setBackground(BG_INPUT);

        String nombreClase = txtNombreProblema.getText().trim();
        String ficheroDatos = txtFicheroDatos.getText().trim();

        // Validar nombre de clase
        if (nombreClase.isEmpty()) {
            txtNombreProblema.setBackground(new Color(0xFF, 0xCC, 0xCC));
            mostrarDialogo("Error en UserProblem",
                "El campo 'Clase' no puede estar vacio.", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String nombreNormalizado = Character.toUpperCase(nombreClase.charAt(0)) + nombreClase.substring(1);
        String claseCompleta = "problema." + nombreNormalizado;

        try {
            Class.forName(claseCompleta);
        } catch (ClassNotFoundException e) {
            txtNombreProblema.setBackground(new Color(0xFF, 0xCC, 0xCC));
            mostrarDialogo("Clase no encontrada",
                "No se encontro la clase: " + claseCompleta + "\n" +
                "Asegurate de que existe en el paquete 'problema'.",
                JOptionPane.ERROR_MESSAGE);
            ok = false;
        }

        if (!ficheroDatos.isEmpty()) {
            java.io.File f = new java.io.File("data" + java.io.File.separator + ficheroDatos);
            if (!f.exists()) {
                txtFicheroDatos.setBackground(new Color(0xFF, 0xCC, 0xCC));
                mostrarDialogo("Fichero no encontrado",
                    "No se encontro el fichero de datos: data/" + ficheroDatos + "\n" +
                    "Asegurate de que esta en la carpeta data/ del proyecto.",
                    JOptionPane.ERROR_MESSAGE);
                ok = false;
            }
        }

        return ok;
    }

}