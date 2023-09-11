package ru.urvanov.itdating;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class LoadPeopleFrame extends JFrame {

    private static final long serialVersionUID = 1675736903555379742L;

    public static final int BATCH_SIZE = 50;
    
    private Settings settings;
    
    private JProgressBar countProgressBar;
    
    private JLabel warningLabel;
    
    private JPanel progressPanel;
    
    private JLabel countLabel;
    
    private JProgressBar prepareProgressBar;
    
    private JLabel prepareLabel;
    
    private JProgressBar searchProgressBar;
    
    private JLabel searchLabel;
    
    private JLabel filterLabel;
    
    private JProgressBar filterProgressBar; 
    
    private JProgressBar checkBlogProgressBar;
    
    private JLabel checkBlogLabel;
    
    private JProgressBar downloadAvatarProgressBar;
    
    private JLabel downloadAvatarLabel;
    
    private JProgressBar saveProgressBar;
    
    private JLabel saveLabel;
    
    private LoadWorker worker;
    
    
    private int totalCount = 0;
    
    public LoadPeopleFrame(Settings settings) {
        this.settings = settings;
        setTitle("Loading");
        setLocationByPlatform(true);
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(warningLabel = new JLabel("""
                        <html>
                        <b>Warning!</b> The process can be <i>very long</i>. Just wait... 
                        The progress bars below show individual progress of 
                        separated tasks. Every task except <b>'Saving'</b> 
                        produces input for next tasks, so their progress can
                         go back when they receive it.
                         </html>"""));
        add(progressPanel = new JPanel());
        GridBagConstraints warningLabelConstraints = new GridBagConstraints();
        warningLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
        warningLabelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        warningLabelConstraints.fill = GridBagConstraints.BOTH;
        warningLabelConstraints.weightx = 1.0;
        gridBagLayout.setConstraints(warningLabel, warningLabelConstraints);
        GridBagConstraints progressPanelConstraints = new GridBagConstraints();
        progressPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        progressPanelConstraints.fill = GridBagConstraints.BOTH;
        progressPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        progressPanelConstraints.gridheight = GridBagConstraints.REMAINDER;
        progressPanelConstraints.weightx = 1.0;
        progressPanelConstraints.weighty = 1.0;
        gridBagLayout.setConstraints(progressPanel, progressPanelConstraints);
        progressPanel.setLayout(new GridLayout(0, 1));
        progressPanel.add(countLabel = new JLabel("Counting..."));
        progressPanel.add(countProgressBar = new JProgressBar());
        progressPanel.add(prepareLabel = new JLabel("Please, wait..."));
        progressPanel.add(prepareProgressBar = new JProgressBar());
        progressPanel.add(searchLabel = new JLabel("Searching girls in GitHub..."));
        progressPanel.add(searchProgressBar = new JProgressBar());
        progressPanel.add(filterLabel = new JLabel("Filtering search results..."));
        progressPanel.add(filterProgressBar = new JProgressBar());
        progressPanel.add(checkBlogLabel = new JLabel("Checking blog..."));
        progressPanel.add(checkBlogProgressBar = new JProgressBar());
        progressPanel.add(downloadAvatarLabel = new JLabel("Downloading avatars..."));
        progressPanel.add(downloadAvatarProgressBar = new JProgressBar());
        progressPanel.add(saveLabel = new JLabel("Saving..."));
        progressPanel.add(saveProgressBar = new JProgressBar());
        
        
        checkBlogLabel.setText("Checking blog url...");
        downloadAvatarLabel.setText("Downloading avatar...");
        countProgressBar.setMaximum(WomanNames.WOMAN_NAMES.size());
    }
    
    @Override
    protected void processWindowEvent(final WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            worker.cancel(false);
        }
        super.processWindowEvent(e);
    }
    
   
    record CountChunk( int totalCount) {};
    
    record PrepareChunk() {};
    
    record SearchChunk (int count) {}
    
    record FilterChunk () {};
    
    
    
    record CheckBlogChunk() {}
    
    record DownloadAvatarChunk() {}
    
    record SaveChunk() {}
    
    record LoadChunk(CountChunk countChunk, PrepareChunk prepareChunk, SearchChunk searchChunk, FilterChunk filterChunk, CheckBlogChunk checkBlogChunk, DownloadAvatarChunk downloadAvatarChunk, SaveChunk saveChunk) {};
    
    public  static void loadPeople(Settings settings)
            throws IOException, SQLException {
        LoadPeopleFrame progressFrame = new LoadPeopleFrame(settings);
        progressFrame.setVisible(true);
        String lastToken = settings.getGitHubAppInstallationToken();
        String token = JOptionPane.showInputDialog(
                progressFrame,
                "Generate GitHub personal access token on https://github.com/settings/tokens and insert it here:",
                lastToken);
        if (!lastToken.equals(token)) {
            settings.setGitHubAppInstallationToken(token);
            settings.save();
        }
        progressFrame.load();
    }

    private void load() {
        totalCount = 0;
        worker = new LoadWorker(settings.getGitHubAppInstallationToken()) {
            
            @Override
            protected void process(List<LoadChunk> chunks) {
                super.process(chunks);
                List<CountChunk> countChunks = new ArrayList<>();
                PrepareChunk prepareChunk = null;
                List<SearchChunk> searchChunks = new ArrayList<>();
                List<FilterChunk> filterChunks = new ArrayList<>();
                List<CheckBlogChunk> checkBlogChunks = new ArrayList<>();
                List<DownloadAvatarChunk> downloadAvatarChunks = new ArrayList<>();
                List<SaveChunk> saveChunks = new ArrayList<>();
                for (LoadChunk chunk : chunks) {
                    //System.out.println("chunk = " + chunk);
                    if (chunk.countChunk()!= null) {
                        countChunks.add(chunk.countChunk());
                    }
                    if (chunk.prepareChunk() != null) {
                        prepareChunk = chunk.prepareChunk();
                    }
                    if (chunk.searchChunk() != null) {
                        searchChunks.add(chunk.searchChunk());
                    }
                    if (chunk.filterChunk() != null) {
                        filterChunks.add(chunk.filterChunk());
                    }
                    if (chunk.checkBlogChunk()!= null) {
                        checkBlogChunks.add(chunk.checkBlogChunk());
                    }
                    if (chunk.downloadAvatarChunk() != null) {
                        downloadAvatarChunks.add(chunk.downloadAvatarChunk());
                    }
                    if (chunk.saveChunk() != null) {
                        saveChunks.add(chunk.saveChunk());
                    }
                    
                }
                
                if (!countChunks.isEmpty()) {
                    countProgressBar.setValue(countProgressBar.getValue() + countChunks.size());
                    totalCount += countChunks.stream().mapToInt(CountChunk::totalCount).sum();
                    searchProgressBar.setMaximum(totalCount);
                    filterProgressBar.setMaximum(totalCount);
                    checkBlogProgressBar.setMaximum(totalCount);
                    downloadAvatarProgressBar.setMaximum(totalCount);
                    saveProgressBar.setMaximum(totalCount);
                }
                if (prepareChunk != null) {
                    prepareProgressBar.setIndeterminate(true);
                    //totalCount = Math.min(LoadWorker.MAX, totalCount);
                    
                    
                    
                }
                if (!searchChunks.isEmpty()) {
                    searchProgressBar.setValue(searchProgressBar.getValue() + searchChunks.stream().mapToInt(SearchChunk::count).sum());
                    prepareProgressBar.setIndeterminate(false);
                    prepareProgressBar.setMaximum(1);
                    prepareProgressBar.setValue(1);
                }
                if (!filterChunks.isEmpty()) {
                    filterProgressBar.setValue(filterProgressBar.getValue() + filterChunks.size());
                }
                if (!checkBlogChunks.isEmpty()) {
                    checkBlogProgressBar.setValue(checkBlogProgressBar.getValue() + checkBlogChunks.size());
                }
                if (!downloadAvatarChunks.isEmpty()) {
                    downloadAvatarProgressBar.setValue(downloadAvatarProgressBar.getValue() + downloadAvatarChunks.size());
                }
                if (!saveChunks.isEmpty()) {
                    saveProgressBar.setValue(saveProgressBar.getValue() + saveChunks.size());
                }
            }
            
            @Override
            protected void done() {
                System.out.println("People load completed.");
                JOptionPane.showMessageDialog(LoadPeopleFrame.this, "Loading has been completed.", "Success", JOptionPane.INFORMATION_MESSAGE);
                LoadPeopleFrame.this.setVisible(false);
                try {
                    this.get();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        };
        worker.execute();
    }

}
