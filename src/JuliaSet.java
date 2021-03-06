import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class JuliaSet extends JFrame {

	final static int XSIZE = 1080;
	final static int YSIZE = 1080;
	final static int X = 400;
	final static int Y = 200;
	final static double zoomStep = 0.05;

	static Complex c = new Complex(-0.4, 0.65);
	static int max = 100;
	static double maxRange = 1.5;
	static double minRange = -1.5;
	static double range = maxRange + Math.abs(minRange);

	static boolean stripColors = false;
	static boolean switchColors = false;
	static int curIndex = 7;

	static BufferedImage im = new BufferedImage(XSIZE, YSIZE, BufferedImage.TYPE_INT_RGB);
	static JuliaSet jS = new JuliaSet();
	static colorHashMap chm = new colorHashMap(10, 10, 10);

	// Menu items

	private JLabel mapF = new JLabel();
	private JMenuBar menuBar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenu newSubMenu = new JMenu("New");
	private JMenu colorSettings = new JMenu("Colors");
	private JMenu moreMenu = new JMenu("Other");
	private JMenuItem generate = new JMenuItem("Generate");
	private JMenuItem presets = new JMenuItem("Presets");
	private JMenuItem save = new JMenuItem("Save as PNG");
	private JMenuItem savepreset = new JMenuItem("Create preset");
	private JMenuItem switchColorPattern = new JMenuItem("Iteration-based coloring");
	private JMenuItem disableColors = new JMenuItem("Disable colors");
	private JMenuItem setMax = new JMenuItem("Generation");
	private JMenuItem quit = new JMenuItem("Quit");
	private JMenuItem about = new JMenuItem("About");
	private JMenuItem learnmore = new JMenuItem("Learn more");
	String[] presetsSelection = new String[20];

	// Default presets and empty list

	static ArrayList<Preset> presetsList;

	Preset rabbit = new Preset("Douady's Rabbit", -0.12, 0.745, -1.5, 1.5);
	Preset sanmaro = new Preset("San Maro", -0.75, 0, -1.5, 1.5);
	Preset dendrite = new Preset("Dendrite", 0, 1, -1.5, 1.5);
	Preset siegeldisk = new Preset("Siegel Disk", -0.390541, 0.586788, -1.5, 1.5);
	Preset cf1 = new Preset("Cool fractal - 1", -0.4, 0.65, -1.5, 1.5);
	Preset cf2 = new Preset("Cool fractal - 2", -0.4, 0.6, -1.5, 1.5);
	Preset cf3 = new Preset("Cool fractal - 3", -0.8, 0.156, -1.5, 1.5);

	public JuliaSet() {
		Panel jSPanel = new Panel();
		presetsList = new ArrayList<Preset>();
		loadDefaultPresets();
		customMethods.updateJLabel(mapF);
		mapF.setHorizontalAlignment(SwingConstants.RIGHT);
		mapF.setFont(new Font("Segoe UI", Font.BOLD, 11));
		mapF.setForeground(Color.white);
//		getContentPane().add(mapF, BorderLayout.SOUTH);
		setBounds(X, Y, XSIZE, YSIZE);
		setVisible(true);
		setTitle("Quadratic Julia Set Visualizer for " + mapF.getText());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(jSPanel);
		getContentPane().add(menuBar, BorderLayout.NORTH);
		menuBar.add(fileMenu);
		fileMenu.add(newSubMenu);
		fileMenu.add(colorSettings);
		colorSettings.add(switchColorPattern);
		colorSettings.add(disableColors);
		fileMenu.add(setMax);
		fileMenu.add(save);
		fileMenu.add(savepreset);
		fileMenu.add(quit);
		newSubMenu.add(generate);
		newSubMenu.add(presets);
		menuBar.add(moreMenu);
		moreMenu.add(about);
		moreMenu.add(learnmore);

		// Mouse listeners

		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() < 0) {
					maxRange = maxRange - zoomStep;
					minRange = minRange + zoomStep;
					range = customMethods.calculateRange(minRange, maxRange);
					generateImage();
				} else {
					maxRange = maxRange + zoomStep;
					minRange = minRange - zoomStep;
					range = customMethods.calculateRange(minRange, maxRange);
					generateImage();
				}
			}
		});
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseMotionListener(new MouseAdapter() {
			int previousY;

			@Override
			public void mousePressed(MouseEvent e) {
				previousY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int y = e.getY();
				if (y < previousY) {
					minRange = customMethods.shift(minRange, -0.05);
					maxRange = customMethods.shift(maxRange, -0.05);
					range = customMethods.calculateRange(minRange, maxRange);
					generateImage();
				}
				if (y > previousY) {
					minRange = customMethods.shift(minRange, 0.05);
					maxRange = customMethods.shift(maxRange, 0.05);
					range = customMethods.calculateRange(minRange, maxRange);
					generateImage();
				}
				previousY = y;
			}
		});

		// Menu actions listeners

		generate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean isNum = false;
				double rd = 0, id = 0;
				String r = null, i = null;
				while (!isNum) {
					r = JOptionPane.showInputDialog("Please enter the real part of c:");
					try {
						if (r != null)
							rd = Double.parseDouble(r);
						isNum = true;
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null, "Please enter a number.");
					}
				}
				if (r != null)
					isNum = false;
				while (!isNum) {
					i = JOptionPane.showInputDialog("Please enter the imaginary part of c:");
					try {
						if (i != null)
							id = Double.parseDouble(i);
						isNum = true;
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null, "Please enter a number.");
					}
				}
				if (r != null && i != null) {
					maxRange = 1.5;
					minRange = -1.5;
					range = customMethods.calculateRange(minRange, maxRange);
					c.setReal(rd);
					c.setImaginary(id);
					customMethods.updateJLabel(mapF);
					setTitle("Quadratic Julia Set Visualizer for " + mapF.getText());
					generateImage();
				}
			}
		});
		presets.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String s = (String) JOptionPane.showInputDialog(null, "Select a preset:", "Presets",
						JOptionPane.PLAIN_MESSAGE, null, presetsSelection, "Douady's Rabbit");
				if (s != null) {
					for (String preset : presetsSelection) {
						if ((preset != null && preset.equals(s))) {
							Preset selectedPreset = findPreset(preset);
							maxRange = selectedPreset.getMaxRange();
							minRange = selectedPreset.getMinRange();
							range = customMethods.calculateRange(minRange, maxRange);
							c.setReal(selectedPreset.getReal());
							c.setImaginary(selectedPreset.getImaginary());
							customMethods.updateJLabel(mapF);
							setTitle("Quadratic Julia Set Visualizer for " + mapF.getText());
							generateImage();
						}
					}
				}
			}
		});
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveImage();
			}

		});
		savepreset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (curIndex == 20) {
					JOptionPane.showMessageDialog(null, "You have reached the maximum number of presets allowed.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				String name = JOptionPane.showInputDialog(null, "Please enter the name of the preset:");
				double realValue = c.getReal();
				double imValue = c.getImaginary();
				addPreset(name, realValue, imValue);
			}
		});
		disableColors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				stripColors ^= true;
				if (stripColors) {
					disableColors.setText("Enable colors");
					mapF.setForeground(Color.black);
				} else {
					disableColors.setText("Disable colors");
					mapF.setForeground(Color.white);
				}
				generateImage();
			}
		});
		switchColorPattern.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switchColors ^= true;
				if (switchColors) {
					switchColorPattern.setText("Layers coloring");
				} else {
					switchColorPattern.setText("Iteration-based coloring");
				}
				generateImage();
			}
		});
		setMax.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isNum = false;
				double rd = 0;
				String r = null;
				while (!isNum) {
					r = JOptionPane.showInputDialog(
							"Please enter the maximum number of iterations per pixels (Recommended 100 - 1000):");
					try {
						if (r != null)
							rd = Double.parseDouble(r);
						max = (int) rd;
						isNum = true;
					} catch (NumberFormatException e1) {
						JOptionPane.showMessageDialog(null, "Please enter a number.");
					}
				}
				if (r != null) {
					generateImage();
				}
			}
		});
		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		// z(n+1) = z(n)^2 + c.
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(null, "Given a complex number c and a complex number z, let z" + '\u2099'
						+ '\u208A' + '\u2081' + " = " + "z" + '\u2099' + '\u00B2' + " + c"
						+ "\nIf the function does not diverge as n tends to infinity, then z \u2099 belongs to the filled-in Julia Set."
						+ "\nThese pseudofractals are generated in the [-1.5, 1.5] range by default with 100 iterations for each pixel."
						+ "\n\n Iterative-based coloring helps indicate which regions diverge the slowest ."
						+ "\n\n Please click the Learn more button to be taken to the Wikipedia page for the subject."
						+ "\n\n", "About Quadratic Julia sets", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		learnmore.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Desktop d = Desktop.getDesktop();
				try {
					d.browse(new URI("https://en.wikipedia.org/wiki/Julia_set"));
				} catch (IOException e1) {
					JOptionPane.showInputDialog(e1.getMessage());
				} catch (URISyntaxException e1) {
					JOptionPane.showInputDialog(e1.getMessage());
				}
			}
		});
	}

	// Main

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		NimbusLookAndFeel theme = new NimbusLookAndFeel();
		UIManager.setLookAndFeel(theme);
		UIManager.put("control", new Color(238, 238, 238));
		UIManager.put("nimbusBase", new Color(238, 238, 238));
		theme.getDefaults().put("defaultFont", new Font("Segoe UI", Font.BOLD, 12));
		generateImage();
	}

	// Classic escape algorithm

	public static void generateImage() {
		int i = 0;
		for (int y = 0; y < im.getHeight(); y++) {
			for (int x = 0; x < im.getWidth(); x++) {
				double c_r = (range / XSIZE) * x - Math.abs(minRange);
				double c_i = (range / YSIZE) * y - Math.abs(maxRange);
				Complex n = new Complex(c_r, c_i);
				Complex temp = n;
				i = 0;
				while ((n.getReal() * n.getReal() + n.getImaginary() * n.getImaginary()) <= (2 * 2) && i < max) {
					temp = (n.multiply(n)).add(c);
					n = temp;
					i++;
				}
				if (!stripColors)
					im.setRGB(x, y,
							(new Color(chm.getColor(45 * i % chm.getSize()).getR(),
									chm.getColor(45 * i % chm.getSize()).getG(),
									chm.getColor(15 * i % chm.getSize()).getB())).getRGB());
				if (stripColors) {
					if (i <= max * 0.9)
						im.setRGB(x, y, 0xffffff);
					else
						im.setRGB(x, y, 0x00000);
				}
				if (switchColors) {
					im.setRGB(x, y,
							(new Color(chm.getColor(i % chm.getSize()).getR(),
									chm.getColor(45 * i % chm.getSize()).getG(),
									chm.getColor(i % chm.getSize()).getB())).getRGB());
				}
				if (i > max * 0.9 && !stripColors)
					im.setRGB(x, y, 0x0b38ff);
				jS.repaint();
			}
		}
	}

	// Presets

	public void loadDefaultPresets() {
		presetsSelection[0] = "Douady's Rabbit";
		presetsSelection[1] = "Dendrite";
		presetsSelection[2] = "San Maro";
		presetsSelection[3] = "Siegel Disk";
		presetsSelection[4] = "Cool fractal - 1";
		presetsSelection[5] = "Cool fractal - 2";
		presetsSelection[6] = "Cool fractal - 3";

		presetsList.add(rabbit);
		presetsList.add(dendrite);
		presetsList.add(sanmaro);
		presetsList.add(siegeldisk);
		presetsList.add(cf1);
		presetsList.add(cf2);
		presetsList.add(cf3);
	}

	public void addPreset(String name, double r, double i) {
		Preset newPreset = new Preset(name, r, i, -2, 2);
		presetsList.add(newPreset);
		addToSelection(newPreset);
	}

	public void addToSelection(Preset newPreset) {
		presetsSelection[curIndex] = newPreset.getName();
		curIndex++;
	}

	public static Preset findPreset(String name) {
		for (Preset p : presetsList) {
			if ((p.getName()).equals(name)) {
				return p;
			}
		}
		return null;
	}

	// Saving

	public static void saveImage() {
		try {
			File output = null;
			fileChooser fc = new fileChooser();
			if (fc.getPath() != null) {
				output = new File(fc.getPath() + "/julia-set.png");
				ImageIO.write(im, "png", output);
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}

	public static class fileChooser {
		JButton save = new JButton("Save");
		JFileChooser fc = new JFileChooser();
		boolean b = false;

		public fileChooser() {
			fc.setDialogTitle("Save");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setApproveButtonText("Save");
			if (fc.showOpenDialog(save) == JFileChooser.APPROVE_OPTION) {
				b = true;
			}
		}

		public String getPath() {
			if (b)
				return fc.getSelectedFile().getAbsolutePath();
			else
				return null;
		}
	}

	public static class Panel extends JPanel {

		JLabel imageIcon = new JLabel(new ImageIcon(im));

		public Panel() {

			setLayout(new BorderLayout());
			add(imageIcon, BorderLayout.CENTER);
		}
	}
}
