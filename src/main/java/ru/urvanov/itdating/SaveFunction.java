package ru.urvanov.itdating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.sql.rowset.serial.SerialBlob;

public class SaveFunction implements Function<DownloadAvatarResult, SaveResult> {

    private AtomicInteger idCounter;
    
    public SaveFunction(AtomicInteger idCounter) {
        this.idCounter = idCounter;
    }
    @Override
    public SaveResult apply(DownloadAvatarResult person) {
        if (person.good()) {
            try (Connection connection = App.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement("insert into people(id, login, name, avatarurl, avatardata, email, blog, location, bio) values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setInt(1, idCounter.getAndIncrement());
                preparedStatement.setString(2, person.login());
                preparedStatement.setString(3, person.name());
                preparedStatement.setString(4, person.avatarUrl());
                preparedStatement.setBlob(5, new SerialBlob(person.avatarData()));
                preparedStatement.setString(6, person.email());
                preparedStatement.setString(7, person.blog());
                preparedStatement.setString(8, person.location());
                preparedStatement.setString(9, person.bio());
                preparedStatement.execute();
                connection.commit();
                return new SaveResult(true, null);
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
                return new SaveResult(false, sqlex);
            }
        }
        return new SaveResult(false, null);
        
    }

}
