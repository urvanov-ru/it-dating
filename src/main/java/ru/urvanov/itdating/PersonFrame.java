package ru.urvanov.itdating;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

public class PersonFrame extends JFrame {

    private static final long serialVersionUID = 2870470575990468753L;
    
    private Settings settings;
    
    private JButton nextPerson;
    
    private JButton openGitHub;
    
    private JButton openBlog;
    
    private JButton loadPeople;
    
    private GridBagLayout gbl;
    
    private JLabel avatar;
    
    private JLabel loginValue;
    
    private JLabel nameValue;
    
    private JLabel emailValue;
    
    private JLabel blogValue;
    
    private JLabel locationValue;
    
    private JLabel bioValue;
    
    private Person person;
    
    public PersonFrame(Settings settings) {
        this.settings = settings;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 800);
        setLocationByPlatform(true);
        setTitle("Person");
    
        gbl = new GridBagLayout();
        setLayout(gbl);
        
        addFullLine(new JLabel());
        
        nextPerson = new JButton("next person");
        nextPerson.addActionListener(this::nextPerson);
        addFullLine(nextPerson);
        
        openGitHub = new JButton("Open GitHub");
        openGitHub.addActionListener(this::openGitHub);
        addFullLine(openGitHub);
        
        openBlog = new JButton("Open blog");
        openBlog.addActionListener(this::openBlog);
        addFullLine(openBlog);
        
        loadPeople = new JButton("load people");
        loadPeople.addActionListener(this::loadPeople);
        addFullLine(loadPeople);
        
        addFullLine(avatar = new JLabel());

        addLabel(new JLabel(), "Login: ");
        addValue(loginValue = new JLabel());
        
        addLabel(new JLabel(), "Name: ");
        addValue(nameValue = new JLabel());
        
        addLabel(new JLabel(), "e-mail: ");
        addValue(emailValue = new JLabel());
        
        addLabel(new JLabel(), "Blog: ");
        addValue(blogValue = new JLabel());
        
        addLabel(new JLabel(), "Location: ");
        addValue(locationValue = new JLabel());
        
        addLabel(new JLabel(), "Bio: ");
        addValue(bioValue = new JLabel());
        
        setPerson(Person.DUMMY);
    }

    private void addFullLine(JComponent component) {
        GridBagConstraints c;
        add(component);
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.1;
        gbl.setConstraints(component, c);
    }
    
    private void addLabel( JLabel label, String text) {
        label.setText(text);
        add(label);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 0.0;
        c.weighty = 0.1;
        gbl.setConstraints(label, c);
    }
    
   private void addValue(JLabel label) {
       add(label);
       GridBagConstraints c = new GridBagConstraints();
       c.anchor = GridBagConstraints.NORTHWEST;
       c.fill = GridBagConstraints.HORIZONTAL;
       c.gridwidth = GridBagConstraints.REMAINDER;
       c.weightx = 1.0;
       c.weighty = 0.1;
       gbl.setConstraints(label, c);
    }

    
    private void nextPerson(ActionEvent event) {
        SwingWorker<Person, Void> worker = new SwingWorker<>() {

            @Override
            protected Person doInBackground() throws Exception {
                Person result = Person.DUMMY;
                System.out.println("Getting next person from the database...");
                try (Connection connection = App.getConnection()) {
                    int max = 0;
                    try (Statement countStatement = connection.createStatement(); ResultSet rs = countStatement.executeQuery("select count(*) from people")) {
                        while (rs.next()) {
                            max = (int)(rs.getLong(1));
                        }
                    }
                    Random random = new Random();
                    int id = random.nextInt(max);
                    try (Statement listStatement = connection.createStatement(); ResultSet rs = listStatement.executeQuery("select login, name, avatarurl, avatardata, email, blog, location, bio from people where id = " + id)) {
                        while (rs.next()) {
                            result = new Person(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getBlob(4).getBinaryStream().readAllBytes(), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8));
                            System.out.println("Got from database " + result);
                        }
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return result;
            }
            @Override
            protected void done() {
                System.out.println("Next person done.");
                enableButtons();
                try {
                    setPerson(this.get());
                } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        disableButtons();
        worker.execute();
    }
    
    protected void setPerson(Person person) {
        this.person = person;
        loginValue.setText(person.login());
        nameValue.setText(person.name());
        avatar.setIcon(person.avatarData() == null ? null : new ImageIcon(person.avatarData()));
        emailValue.setText(person.email());
        blogValue.setText(person.blog());
        locationValue.setText(person.location());
    }

    private void enableButtons() {
        nextPerson.setEnabled(true);
    }
    
    private void disableButtons() {
        nextPerson.setEnabled(false);
    }
    
    private void openGitHub(ActionEvent event) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("https://github.com/" + loginValue.getText()));
            }
        } catch (IOException | URISyntaxException ioex) {
            ioex.printStackTrace();
        }
    }
    
    private void openBlog(ActionEvent event) {
        try {
            if (Desktop.isDesktopSupported()) {
                String blogUri = blogValue.getText().strip();
                if (!blogUri.startsWith("http")) {
                    blogUri = "https://" + blogUri;
                }
                Desktop.getDesktop().browse(new URI(blogUri));
            }
        } catch (IOException | URISyntaxException ioex) {
            ioex.printStackTrace();
        }
    }
    
    private void loadPeople(ActionEvent event) {
        try {
            LoadPeopleFrame.loadPeople(settings);
        } catch (IOException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
