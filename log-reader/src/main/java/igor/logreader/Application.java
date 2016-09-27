package igor.logreader;

import static java.lang.String.format;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

@SuppressWarnings("serial")
public class Application extends JFrame implements Runnable, ActionListener, KeyListener {

	private JTextArea logArea;
	private JTextArea searchArea;
	TitledBorder logtitledBorder = BorderFactory.createTitledBorder("Log File");
	TitledBorder searchtitledBorder = BorderFactory.createTitledBorder("Search Results");
	JTextField searchField = new JTextField();
	Highlighter highlighter = new DefaultHighlighter();
	ExecutorService regexThread = Executors.newFixedThreadPool(1);
	AtomicBoolean searchInProgress = new AtomicBoolean();
	HighlightPainter yellowHighlight = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
	HighlightPainter greenHighlight = new DefaultHighlighter.DefaultHighlightPainter(Color.green);
	String[] lines;

	JPanel status = new JPanel();
	JLabel searchStatusMessage = new JLabel();
	JLabel fileStatusMessage = new JLabel();

	int highlights;
	int highlightedLines;

	public Application() {
		initUI();
	}

	private void initUI() {
		// ImageIcon webIcon = new ImageIcon("web.png");

		// setIconImage(webIcon.getImage());
		setTitle("Simple example");
		setSize(900, 600);
		setPreferredSize(new Dimension(1200, 800));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		JToolBar toolBar = new JToolBar("Still draggable");
		add(toolBar, BorderLayout.PAGE_START);

		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		add(main, BorderLayout.CENTER);

		logArea = new JTextArea();
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		logArea.setEditable(false);
		logArea.setHighlighter(highlighter);

		searchArea = new JTextArea();
		searchArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		searchArea.setEditable(false);

		JScrollPane logScroller = new JScrollPane(logArea);
		Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		logScroller.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(logtitledBorder, emptyBorder), logScroller.getBorder()));
		main.add(logScroller);
		// main.add(Box.createVerticalStrut(10));
		JPanel searchBar = new JPanel();
		searchBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		main.add(searchBar);

		JLabel searchLabel = new JLabel("Search Text:");
		searchBar.add(searchLabel);

		searchBar.add(searchField);
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		searchField.setMinimumSize(new Dimension(100, 25));
		searchField.setPreferredSize(new Dimension(1000, 25));
		searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
		searchBar.setBorder(BorderFactory.createEtchedBorder());
		searchField.addKeyListener(this);

		JButton load = new JButton("Load");
		searchBar.add(load);
		load.addActionListener(this);
		// main.add(Box.createVerticalStrut(10));
		JScrollPane searchScroller = new JScrollPane(searchArea);
		searchScroller
				.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(searchtitledBorder, emptyBorder), searchScroller.getBorder()));
		main.add(searchScroller);
		searchScroller.setMaximumSize(new Dimension(logScroller.getMaximumSize().width / 3, logScroller.getMaximumSize().height / 3));
		searchScroller.setMinimumSize(new Dimension(logScroller.getMinimumSize().width / 3, logScroller.getMinimumSize().height / 3));
		searchScroller.setPreferredSize(new Dimension(logScroller.getPreferredSize().width / 3, logScroller.getPreferredSize().height / 3));

		add(status, BorderLayout.PAGE_END);

		status.setLayout(new FlowLayout());
		status.add(searchStatusMessage);
		status.add(fileStatusMessage);
		status.setBorder(BorderFactory.createLoweredSoftBevelBorder());

		pack();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Application ex = new Application();
			ex.setVisible(true);
		});

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean accept(File f) {
				if (f.getName().startsWith(".")) {
					return false;
				}
				if (f.isDirectory()) {
					return true;
				}
				int i = f.getName().lastIndexOf('.');
				String ext = f.getName().substring(i + 1).toLowerCase();
				return ext.equals("txt") || ext.equals("log");
			}
		});
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			Future<Boolean> done = Executors.newFixedThreadPool(1).submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					logArea.read(new InputStreamReader(new FileInputStream(file)), null);

					Document document = logArea.getDocument();
					lines = document.getText(0, document.getLength()).split("\n");
					long size = file.length();

					fileStatusMessage.setText(format("Lines: %d, Size: %d", lines.length, size));
					return Boolean.TRUE;
				}
			});
			try {
				done.get();
				SwingUtilities.invokeLater(() -> {
					logtitledBorder.setTitle(file.getName());
					repaint();
				});

			} catch (InterruptedException | ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		searchInProgress.set(false);
		String word = searchField.getText();
		if (word.isEmpty()) {
			return;
		}
		if (logArea.getText().isEmpty()) {
			return;
		}
		regexThread.execute(new Runnable() {

			@Override
			public void run() {
				try {
					searchInProgress.set(true);
					Thread.sleep(200);
					String word = searchField.getText();
					if (word.isEmpty()) {
						return;
					}
					highlighter.removeAllHighlights();
					searchArea.setText(null);
					Pattern regex = Pattern.compile("(" + word + ")");
					int len = 0;
					highlights = 0;
					highlightedLines = 0;
					for (int i = 0; i < lines.length; i++) {
						if (!searchInProgress.get()) {
							break;
						}
						Matcher matcher = regex.matcher(lines[i]);
						boolean matches = false;
						while (matcher.find()) {
							matches = true;
							int p0 = len + matcher.start(1);
							int p1 = p0 + word.length();
							highlighter.addHighlight(p0, p1, yellowHighlight);
							highlights++;
							searchArea.append(lines[i] + "\n");
						}
						if (matches) {
							highlighter.addHighlight(len, len + lines[i].length(), greenHighlight);
							highlightedLines++;
						}
						len += lines[i].length() + 1;
					}
					searchStatusMessage.setText(format("Highlights: %d, Lines: %d", highlights, highlightedLines));
				} catch (BadLocationException | InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} finally {
					searchInProgress.set(false);
					SwingUtilities.invokeLater(() -> repaint());
				}
			}
		});

	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

}
