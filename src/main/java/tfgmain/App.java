package tfgmain;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
	public static void main(String[] args) {
		
		// Esto es para que la vetnana se vea como una aplicacion del ssitemas
		// operativo que estamos usando
	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch (Exception ignored) {}

	    // Invoke later para que no se quede pillado
	    SwingUtilities.invokeLater(() -> {
	    	// Llamamos a la ventana para que se vea ponemos el set visible true.
	        VentanaPrincipal v = new VentanaPrincipal();
	        v.setVisible(true);
	    });
	}

}
