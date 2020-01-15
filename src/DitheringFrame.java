import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileSystemView;

public class DitheringFrame extends JFrame {

	private static final long serialVersionUID = 1857163654377315572L;
	String fileName = "";
	BufferedImage image, ditheredImage, displayedImage, displayedDitheredImage;
	ActionListener repaintGPanel = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			graphicPanel.repaint();
		}
	};

	JTextField factorField = new JTextField("255");
	JRadioButton twoDFilter = new JRadioButton("2D error diffusion");
	JRadioButton fsFilter = new JRadioButton("Floyd-Steinberg");
	JRadioButton jjnFilter = new JRadioButton("Jarvis-Judice-Ninke");
	JRadioButton stuckiFilter = new JRadioButton("Stucki");
	JRadioButton burkesFilter = new JRadioButton("Burkes");
	JRadioButton sierra3Filter = new JRadioButton("Sierra-3");
	JRadioButton sierra2Filter = new JRadioButton("Sierra-2");
	JRadioButton sierra24AFilter = new JRadioButton("Sierra-2-4A");
	JRadioButton noFilter = new JRadioButton("No filter");
	JRadioButton grayScale = new JRadioButton("Grayscale");

	static BufferedImage DeepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
	int index(int x, int y, BufferedImage image) {
		return x + y * image.getWidth();
	}

	int minMax(int n, int min, int max) {
		if (n > max)
			return max;
		else if (n < min)
			return min;
		else
			return n;
	}

	BufferedImage rescale(BufferedImage input, int outputWidth, int outputHeight) {
		BufferedImage output = new BufferedImage(outputWidth, outputHeight, input.getType());

		for (int outputX = 0; outputX < outputWidth; outputX++)
			for (int outputY = 0; outputY < outputHeight; outputY++) {
				int inputX = outputX * (int) (outputWidth / (float) input.getWidth());
				int inputY = outputY * (int) (outputHeight / (float) input.getHeight());
				output.setRGB(outputX, outputY, input.getRGB(inputX, inputY));
			}
		return output;
	}

	void propagateError(int x, int y, int offX, int offY, float factor, float[] quantError) {
		if (x + offX > 0 && x + offX < image.getWidth() && y + offY > 0 && y + offY < image.getHeight()) {
			Color c = new Color(image.getRGB(x + offX, y + offY));
			float red = c.getRed() + quantError[0] * factor;
			float green = c.getGreen() + quantError[1] * factor;
			float blue = c.getBlue() + quantError[2] * factor;

			red = minMax((int) red, 0, 255);
			green = minMax((int) green, 0, 255);
			blue = minMax((int) blue, 0, 255);

			ditheredImage.setRGB(x + offX, y + offY, new Color((int) red, (int) green, (int) blue).getRGB());
		}
	}

	JPanel graphicPanel = new JPanel() {
		private static final long serialVersionUID = 278470297026981101L;

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.LIGHT_GRAY);
			g2.fillRect(0, 0, getWidth(), getHeight());

			if (!fileName.equals("")) {
				try {
					image = ImageIO.read(new File(fileName));
				} catch (IOException e) {
					JOptionPane.showConfirmDialog(null, "Image could not be found, try again");
				}
				ditheredImage = image;
				for (int y = 0; y < image.getHeight(); y++)
					for (int x = 0; x < image.getWidth(); x++) {
						Color c = new Color(image.getRGB(x, y));
						float oldRed = c.getRed();
						float oldGreen = c.getGreen();
						float oldBlue = c.getBlue();

						if (grayScale.isSelected()) {
							float gray = (oldRed + oldGreen + oldBlue) / 3;
							oldRed = gray;
							oldGreen = gray;
							oldBlue = gray;
						}

						int factor = Integer.parseInt(factorField.getText());

						// finding closest color on the new palette
						int newRed, newGreen, newBlue;

						newRed = Math.round(factor * oldRed / 255) * (255 / factor);
						newGreen = Math.round(factor * oldGreen / 255) * (255 / factor);
						newBlue = Math.round(factor * oldBlue / 255) * (255 / factor);

						ditheredImage.setRGB(x, y, new Color(newRed, newGreen, newBlue).getRGB());

						// filtering
						float quantErrorRed = oldRed - newRed;
						float quantErrorGreen = oldGreen - newGreen;
						float quantErrorBlue = oldBlue - newBlue;
						float[] quantError = { quantErrorRed, quantErrorGreen, quantErrorBlue };

						if (twoDFilter.isSelected()) {
							propagateError(x, y, 1, 0, 1 / 2f, quantError);
							propagateError(x, y, 0, 1, 1 / 2f, quantError);
						} else if (fsFilter.isSelected()) {
							propagateError(x, y, 1, 0, 7 / 16f, quantError);
							propagateError(x, y, -1, 1, 3 / 16f, quantError);
							propagateError(x, y, 0, 1, 5 / 16f, quantError);
							propagateError(x, y, 1, 1, 1 / 16f, quantError);
						} else if (jjnFilter.isSelected()) {
							propagateError(x, y, 1, 0, 7 / 48f, quantError);
							propagateError(x, y, 2, 0, 5 / 48f, quantError);
							propagateError(x, y, -2, 1, 3 / 48f, quantError);
							propagateError(x, y, -1, 1, 5 / 48f, quantError);
							propagateError(x, y, 0, 1, 7 / 48f, quantError);
							propagateError(x, y, 1, 1, 5 / 48f, quantError);
							propagateError(x, y, 2, 1, 3 / 48f, quantError);
							propagateError(x, y, -2, 2, 1 / 48f, quantError);
							propagateError(x, y, -1, 2, 3 / 48f, quantError);
							propagateError(x, y, 0, 2, 5 / 48f, quantError);
							propagateError(x, y, 1, 2, 3 / 48f, quantError);
							propagateError(x, y, 2, 2, 1 / 48f, quantError);
						} else if (stuckiFilter.isSelected()) {
							propagateError(x, y, 1, 0, 8 / 42f, quantError);
							propagateError(x, y, 2, 0, 4 / 42f, quantError);
							propagateError(x, y, -2, 1, 2 / 42f, quantError);
							propagateError(x, y, -1, 1, 4 / 42f, quantError);
							propagateError(x, y, 0, 1, 8 / 42f, quantError);
							propagateError(x, y, 1, 1, 4 / 42f, quantError);
							propagateError(x, y, 2, 1, 2 / 42f, quantError);
							propagateError(x, y, -2, 2, 1 / 42f, quantError);
							propagateError(x, y, -1, 2, 2 / 42f, quantError);
							propagateError(x, y, 0, 2, 4 / 42f, quantError);
							propagateError(x, y, 1, 2, 2 / 42f, quantError);
							propagateError(x, y, 2, 2, 1 / 42f, quantError);
						} else if (burkesFilter.isSelected()) {
							propagateError(x, y, 1, 0, 8 / 32f, quantError);
							propagateError(x, y, 2, 0, 4 / 32f, quantError);
							propagateError(x, y, -2, 1, 2 / 32f, quantError);
							propagateError(x, y, -1, 1, 4 / 32f, quantError);
							propagateError(x, y, 0, 1, 8 / 32f, quantError);
							propagateError(x, y, 1, 1, 4 / 32f, quantError);
							propagateError(x, y, 2, 1, 2 / 32f, quantError);
						} else if (sierra3Filter.isSelected()) {
							propagateError(x, y, 1, 0, 5 / 32f, quantError);
							propagateError(x, y, 2, 0, 3 / 32f, quantError);
							propagateError(x, y, -2, 1, 2 / 32f, quantError);
							propagateError(x, y, -1, 1, 4 / 32f, quantError);
							propagateError(x, y, 0, 1, 5 / 32f, quantError);
							propagateError(x, y, 1, 1, 4 / 32f, quantError);
							propagateError(x, y, 2, 1, 2 / 32f, quantError);
							propagateError(x, y, -1, 2, 2 / 32f, quantError);
							propagateError(x, y, 0, 2, 3 / 32f, quantError);
							propagateError(x, y, 1, 2, 2 / 32f, quantError);
						} else if (sierra2Filter.isSelected()) {
							propagateError(x, y, 1, 0, 4 / 16f, quantError);
							propagateError(x, y, 2, 0, 3 / 16f, quantError);
							propagateError(x, y, -2, 1, 1 / 16f, quantError);
							propagateError(x, y, -1, 1, 2 / 16f, quantError);
							propagateError(x, y, 0, 1, 3 / 16f, quantError);
							propagateError(x, y, 1, 1, 2 / 16f, quantError);
							propagateError(x, y, 2, 1, 1 / 16f, quantError);
						} else if (sierra24AFilter.isSelected()) {
							propagateError(x, y, 1, 0, 1 / 2f, quantError);
							propagateError(x, y, -1, 1, 1 / 4f, quantError);
							propagateError(x, y, 0, 1, 1 / 4f, quantError);
						}
					}
				displayedImage = image;
				displayedDitheredImage = DeepCopy(ditheredImage);
		/*		if (displayedImage.getWidth() > Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2 - 167 / 2f) {
					displayedImage = rescale(image, (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2f),
							(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / (float) image.getHeight()
									/ 2f));
					displayedDitheredImage = rescale(ditheredImage, (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2f),
							(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / (float) ditheredImage.getHeight()
									/ 2f));
				}
				if (displayedImage.getHeight() > Toolkit.getDefaultToolkit().getScreenSize().getHeight()) {
					displayedImage = rescale(
							image, (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
									/ (float) image.getWidth() / 2f),
							(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2f));
					displayedDitheredImage = rescale(
							ditheredImage, (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
									/ (float) ditheredImage.getWidth() / 2f),
							(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2f));

				}

			*/	g2.drawImage(image, 0, 0, null);
				g2.drawImage(ditheredImage, displayedImage.getWidth() + 1, 0, null);
			}
		}
	};

	public DitheringFrame() throws IOException {
		super("Dithering filters");

		MenuBar menuBar = new MenuBar();
		Menu file = new Menu("Image");
		MenuItem load = new MenuItem("Load");
		MenuItem save = new MenuItem("Save");
		menuBar.add(file);
		file.add(save);
		file.add(load);

		setMenuBar(menuBar);

		load.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
					int returnValue = jfc.showOpenDialog(null);
					if (returnValue == JFileChooser.APPROVE_OPTION) {
						File file = jfc.getSelectedFile();
						image = ImageIO.read(file);
						fileName = file.getAbsolutePath();
						setSize(image.getWidth() * 2 + 157, image.getHeight() + 73);
						if (image.getHeight() < 500)
							setSize(getWidth(), 500);
					}
					graphicPanel.setSize(image.getWidth() * 2, image.getHeight());
					graphicPanel.repaint();
					noFilter.setEnabled(true);
					twoDFilter.setEnabled(true);
					fsFilter.setEnabled(true);
					jjnFilter.setEnabled(true);
					stuckiFilter.setEnabled(true);
					burkesFilter.setEnabled(true);
					sierra24AFilter.setEnabled(true);
					sierra3Filter.setEnabled(true);
					sierra2Filter.setEnabled(true);
					factorField.setEnabled(true);
					grayScale.setEnabled(true);

				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		});

		save.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
					jfc.setDialogTitle("Choose a directory to save your file: ");
					int returnValue = jfc.showSaveDialog(null);
					if (returnValue == JFileChooser.APPROVE_OPTION) {
						// String savedFileName = jfc.getSelectedFile().getName() + ".png";
						ImageIO.write(ditheredImage, "png", new File(jfc.getSelectedFile().getAbsolutePath() + ".png"));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		ButtonGroup bg = new ButtonGroup();
		bg.add(noFilter);
		bg.add(fsFilter);
		bg.add(twoDFilter);
		bg.add(jjnFilter);
		bg.add(stuckiFilter);
		bg.add(burkesFilter);
		bg.add(sierra24AFilter);
		bg.add(sierra2Filter);
		bg.add(sierra3Filter);

		noFilter.setEnabled(false);
		twoDFilter.setEnabled(false);
		fsFilter.setEnabled(false);
		jjnFilter.setEnabled(false);
		stuckiFilter.setEnabled(false);
		burkesFilter.setEnabled(false);
		sierra24AFilter.setEnabled(false);
		sierra3Filter.setEnabled(false);
		sierra2Filter.setEnabled(false);
		factorField.setEnabled(false);
		grayScale.setEnabled(false);

		noFilter.addActionListener(repaintGPanel);
		fsFilter.addActionListener(repaintGPanel);
		twoDFilter.addActionListener(repaintGPanel);
		jjnFilter.addActionListener(repaintGPanel);
		stuckiFilter.addActionListener(repaintGPanel);
		burkesFilter.addActionListener(repaintGPanel);
		sierra24AFilter.addActionListener(repaintGPanel);
		sierra2Filter.addActionListener(repaintGPanel);
		sierra3Filter.addActionListener(repaintGPanel);
		factorField.addActionListener(repaintGPanel);
		factorField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Integer.parseInt(factorField.getText()) < 1 || Integer.parseInt(factorField.getText()) > 255)
					JOptionPane.showMessageDialog(null, "Come on, try putting numbers from 1 to 255");
			}
		});
		grayScale.addActionListener(repaintGPanel);

		noFilter.setSelected(true);

		JPanel buttonsPanel = new JPanel();
		JPanel upperLeftPanel = new JPanel();
		JPanel leftPanel = new JPanel();

		buttonsPanel.setLayout(new GridLayout(10, 1));
		upperLeftPanel.setLayout(new GridLayout(2, 1));
		upperLeftPanel.add(factorField);
		upperLeftPanel.add(grayScale);
		buttonsPanel.add(noFilter);
		buttonsPanel.add(twoDFilter);
		buttonsPanel.add(fsFilter);
		buttonsPanel.add(jjnFilter);
		buttonsPanel.add(stuckiFilter);
		buttonsPanel.add(burkesFilter);
		buttonsPanel.add(sierra3Filter);
		buttonsPanel.add(sierra2Filter);
		buttonsPanel.add(sierra24AFilter);

		add(leftPanel, BorderLayout.WEST);
		leftPanel.setBounds(0, 0, 167, getHeight());
		buttonsPanel.setBorder(BorderFactory.createTitledBorder("Choose filter"));
		upperLeftPanel.setBorder(BorderFactory.createTitledBorder("Choose bits amount"));
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(upperLeftPanel, BorderLayout.NORTH);
		leftPanel.add(buttonsPanel, BorderLayout.CENTER);

		// image = ImageIO.read(new File(fileName));
		setSize(leftPanel.getWidth() + 500, 573);
		setVisible(true);
		add(graphicPanel, BorderLayout.CENTER);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	public static void main(String[] args) {
		try {
			new DitheringFrame();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
