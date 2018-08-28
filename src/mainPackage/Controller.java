package mainPackage;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static java.lang.Math.floor;
import static java.lang.String.format;

public class Controller implements Initializable {

    public Slider timeSlider;
    public Slider volumeSlider;
    public MediaView mediaView;
    public TabPane tabs;
    public Label timeLabel;
    public Canvas canvas1;
    public Canvas canvas2;
    private MediaPlayer mediaPlayer;
    private Media media;
    private boolean loop;
    private boolean mousePressed;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
    }

    private void init() {
        if (mediaPlayer == null || mediaPlayer.getMedia() == null)
            return;

        timeSlider.setOnMousePressed(event -> mousePressed = true);
        timeSlider.setOnMouseReleased(event -> {
            mediaPlayer.seek(Duration.seconds((media.getDuration().toSeconds() * timeSlider.getValue()) / 100));
            mousePressed = false;
        });

        volumeSlider.valueProperty().bindBidirectional(mediaPlayer.volumeProperty());
        volumeSlider.setMax(1);
        volumeSlider.setBlockIncrement(0.1);
        volumeSlider.setValue(0.2);

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            if ((int) newValue.toSeconds() > (int) oldValue.toSeconds()) {
                timeLabel.setText(formatTime(newValue, media.getDuration()));
                if (!mousePressed) {
                    timeSlider.setValue((newValue.toSeconds() / media.getDuration().toSeconds()) * 100);
                }
            }

        });

        canvas1.setRotate(180);
        canvas1.getGraphicsContext2D().setFill(Color.RED);
        canvas2.getGraphicsContext2D().setFill(Color.RED);

        canvas1.widthProperty().bind(tabs.widthProperty());
        canvas1.heightProperty().bind(tabs.heightProperty());
        canvas2.widthProperty().bind(tabs.widthProperty());
        canvas2.heightProperty().bind(tabs.heightProperty());


        mediaPlayer.setAudioSpectrumInterval(0.03);

        mediaPlayer.setAudioSpectrumThreshold(-80);

        canvas1.widthProperty().addListener((observable, oldValue, newValue) -> mediaPlayer.setAudioSpectrumNumBands(newValue.intValue() / 4));

        mediaPlayer.setOnEndOfMedia(() -> {
            if (loop) {
                mediaPlayer.seek(Duration.ZERO);
            }
        });


        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            canvas1.getGraphicsContext2D().clearRect(0, 0, canvas1.getWidth(), canvas1.getHeight());
            canvas2.getGraphicsContext2D().clearRect(0, 0, canvas1.getWidth(), canvas1.getHeight());
            for (int index = 0; index < mediaPlayer.getAudioSpectrumNumBands(); index++) {
                double location = canvas1.getWidth() / mediaPlayer.getAudioSpectrumNumBands() * index;
                double width = canvas1.getWidth() / mediaPlayer.getAudioSpectrumNumBands();
                double height = 80 + magnitudes[index];

                canvas1.getGraphicsContext2D().strokeRect(location, canvas1.getHeight() / 2, width, height);
                canvas2.getGraphicsContext2D().strokeRect(location, canvas2.getHeight() / 2, width, height);
            }

        });

        canvas2.setOnMouseClicked(event -> mediaPlayer.setAudioSpectrumNumBands(3));


        tabs.widthProperty().addListener((observable, oldValue, newValue) -> mediaView.setFitWidth((Double) newValue));
        tabs.heightProperty().addListener(((observable, oldValue, newValue) -> mediaView.setFitHeight((Double) newValue)));


    }

    @FXML
    private void openFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Media", "*.mp3", "*.waw", "*.mp4",
                "*.aac", ".pcm", "*.avc", "*.flv", "*.fxm", "*.wav"));

        File mediaFile = fileChooser.showOpenDialog(new Stage());
        if (mediaFile == null || !mediaFile.exists()) {
            //TODO warn the user that the file they have selected no longer exists
            return;
        }

        playMedia(mediaFile);
    }

    private void playMedia(File mediaFile) {
        media = new Media(mediaFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.getMediaPlayer().play();
        init();
    }

    public void forwardRequested() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(5)));
    }

    public void loopChecked(ActionEvent actionEvent) {
        loop = ((CheckBox) actionEvent.getSource()).isSelected();
    }

    public void ratioChecked(ActionEvent actionEvent) {
        mediaView.setPreserveRatio(((CheckBox) actionEvent.getSource()).isSelected());
    }

    public void backRequested() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(Duration.seconds(5)));

    }

    public void controlRequested(ActionEvent actionEvent) {
        if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
            mediaPlayer.pause();
            ((Button) actionEvent.getSource()).setText(">>");
        } else if (mediaPlayer.getStatus().equals(MediaPlayer.Status.PAUSED)) {
            mediaPlayer.play();
            ((Button) actionEvent.getSource()).setText("||");
        }
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }
}
