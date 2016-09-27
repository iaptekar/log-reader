package igor.logreader;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

@SuppressWarnings("serial")
public class LoadFileAction extends AbstractAction {

	private JTextArea text;

	public LoadFileAction(JTextArea text) {
		this.text = text;
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
		int result = chooser.showOpenDialog((Component) e.getSource());
		if (result == JFileChooser.APPROVE_OPTION) {
			Path file = chooser.getSelectedFile().toPath();
			Executors.newFixedThreadPool(1).execute(new Runnable() {

				@Override
				public void run() {
					try {
						Files.lines(file).forEach(l -> {
							text.append(l);
							text.append("\n");
						});
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			});

		}
	}

}
