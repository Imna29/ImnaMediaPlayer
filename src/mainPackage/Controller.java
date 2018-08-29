package mainPackage;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import static java.lang.Math.floor;
import static java.lang.String.format;

public class Controller implements Initializable {
    @FXML
    private Slider timeSlider;
    @FXML
    private Slider volumeSlider;
    @FXML
    private MediaView mediaView;
    @FXML
    private TabPane tabs;
    @FXML
    private Label timeLabel;
    @FXML
    private Canvas canvas1;
    @FXML
    private Canvas canvas2;
    @FXML
    private ListView directoryList;
    private boolean loop;
    private boolean mousePressed;
    private HashMap<String, File> fileHashMap = new HashMap<>();

    private String audioExtensions[] = {"*.mp3", "*.aif", "*.aiff", "*.wav"};
    private String videoExtensions[] = {"*.mp4", "*.m4a", "*.flv", "*.fxm", "*.m4v"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void init(MediaPlayer mediaPlayer, Media media) {
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

        mediaPlayer.setAudioSpectrumNumBands((int) canvas1.getWidth() / 4);
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
        tabs.widthProperty().addListener((observable, oldValue, newValue) -> mediaView.setFitWidth((Double) newValue));
        tabs.heightProperty().addListener(((observable, oldValue, newValue) -> mediaView.setFitHeight((Double) newValue)));
    }

    @FXML
    private void openFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        String[] extensions = new String[audioExtensions.length + videoExtensions.length];
        System.arraycopy(audioExtensions, 0, extensions, 0, audioExtensions.length);
        System.arraycopy(videoExtensions, 0, extensions, audioExtensions.length, videoExtensions.length);

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Media", extensions));
        File mediaFile = fileChooser.showOpenDialog(new Stage());
        if (mediaFile == null || !mediaFile.exists()) {
            //TODO warn the user that the file they have selected no longer exists
            return;
        }
        playMedia(mediaFile);
        scanDirectory(mediaFile);
    }

    private void scanDirectory(File mediaFile) {
        ArrayList<File> mediaFiles = new ArrayList<>();

        File directory = mediaFile.getParentFile();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                for (String audioEnding : audioExtensions) {
                    if (file.getName().endsWith(audioEnding.substring(1))) {
                        mediaFiles.add(file);
                        break;
                    }
                }
                for (String videoEnding : videoExtensions) {
                    if (file.getName().endsWith(videoEnding.substring(1))) {
                        mediaFiles.add(file);
                        break;
                    }
                }
            }
        }

        populateList(mediaFiles);
    }

    private void populateList(ArrayList<File> files) {
        ObservableList<String> data = FXCollections.observableArrayList();
        for (File file : files) {
            Media media = new Media(file.toURI().toString());
            media.getMetadata().addListener((MapChangeListener<String, Object>) change -> {
                if (change.getKey().equals("title")) {
                    String path = media.getSource().substring(6);
                    fileHashMap.put(change.getValueAdded().toString(), new File(path.replaceAll("%20", " ")));
                    data.add(change.getValueAdded().toString());
                }
            });
        }
        directoryList.setItems(data);
        directoryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            playMedia(fileHashMap.get(newValue.toString()));
        });

    }

    private void playMedia(File mediaFile) {
        if (mediaView.getMediaPlayer() != null) {
            mediaView.getMediaPlayer().dispose();
        }
        Media media = new Media(mediaFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.getMediaPlayer().play();

        toggleMediaTab(true);
        for (String ext : videoExtensions) {
            if (mediaView.getMediaPlayer().getMedia().getSource().endsWith(ext.substring(1))) {
                toggleMediaTab(false);
            }
        }
        init(mediaPlayer, media);
    }

    private void toggleMediaTab(boolean toggle) {
        tabs.getTabs().get(1).setDisable(toggle);
        if (toggle) {
            tabs.getSelectionModel().select(0);
        } else {
            tabs.getSelectionModel().select(1);
        }
    }

    public void forwardRequested() {
        mediaView.getMediaPlayer().seek(mediaView.getMediaPlayer().getCurrentTime().add(Duration.seconds(5)));
    }

    public void loopChecked(ActionEvent actionEvent) {
        loop = ((CheckBox) actionEvent.getSource()).isSelected();
    }

    public void ratioChecked(ActionEvent actionEvent) {
        mediaView.setPreserveRatio(((CheckBox) actionEvent.getSource()).isSelected());
    }

    public void backRequested() {
        mediaView.getMediaPlayer().seek(mediaView.getMediaPlayer().getCurrentTime().subtract(Duration.seconds(5)));

    }

    public void controlRequested(ActionEvent actionEvent) {
        if (mediaView.getMediaPlayer().getStatus().equals(MediaPlayer.Status.PLAYING)) {
            mediaView.getMediaPlayer().pause();
            ((Button) actionEvent.getSource()).setText(">>");
        } else if (mediaView.getMediaPlayer().getStatus().equals(MediaPlayer.Status.PAUSED)) {
            mediaView.getMediaPlayer().play();
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
