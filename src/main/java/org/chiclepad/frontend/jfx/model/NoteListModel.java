package org.chiclepad.frontend.jfx.model;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTimePicker;
import com.jfoenix.effects.JFXDepthManager;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.chiclepad.backend.Dao.DaoFactory;
import org.chiclepad.backend.Dao.NoteDao;
import org.chiclepad.backend.entity.Category;
import org.chiclepad.backend.entity.Note;
import org.chiclepad.constants.ChiclePadColor;
import org.chiclepad.frontend.jfx.ChiclePadApp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class NoteListModel implements ListModel{

    private List<Note> notes;

    private JFXMasonryPane layout;

    private VBox selectedPostIt;

    private Note selectedNote;

    private JFXTextField descriptionField;

    private JFXDatePicker reminderDate;

    private JFXTimePicker reminderTime;

    private NoteDao noteDao;

    private String filter = "";

    public NoteListModel(
            JFXMasonryPane layout,
            JFXTextField descriptionField,
            JFXDatePicker reminderDate,
            JFXTimePicker reminderTime
    ) {
        this.notes = new ArrayList<>();
        this.layout = layout;
        this.descriptionField = descriptionField;
        this.reminderDate = reminderDate;
        this.reminderTime = reminderTime;
        this.noteDao = DaoFactory.INSTANCE.getNoteDao();
    }

    public void add(Note addedNote) {
        notes.add(addedNote);
        addNoteToLayout(addedNote);
    }

    private void addNoteToLayout(Note addedNote) {
        VBox postIt = new VBox();
        stylePostIt(addedNote, postIt);

        postIt.addEventFilter(MouseEvent.MOUSE_PRESSED, selectNote(addedNote, postIt));

        Label bodyText = new Label(addedNote.getContent());
        styleText(bodyText);

        postIt.getChildren().add(bodyText);

        layout.getChildren().add(postIt);
    }

    private EventHandler<MouseEvent> selectNote(Note selectedNote, VBox postIt) {
        return event -> {
            deselectPreviousPostIt();
            selectNewPostIt(selectedNote, postIt);

            setDescriptionTextField();
            setReminderTimeFields();
        };
    }

    private void deselectPreviousPostIt() {
        if (selectedPostIt == null) {
            return;
        }

        noteDao.update(selectedNote);

        selectedPostIt.getStyleClass().removeIf(cssClass -> cssClass.equals("bordered"));
        JFXDepthManager.setDepth(selectedPostIt, 1);
    }

    private void selectNewPostIt(Note addedNote, VBox postIt) {
        if (selectedNote != null) {
            noteDao.update(selectedNote);
        }

        this.selectedPostIt = postIt;
        this.selectedNote = addedNote;

        setSelectedStyles();
    }

    private void setSelectedStyles() {
        selectedPostIt.getStyleClass().add("bordered");
        JFXDepthManager.setDepth(selectedPostIt, 2);
    }

    private void setDescriptionTextField() {
        descriptionField.setText(selectedNote.getContent());
        descriptionField.textProperty().addListener(((observable, oldValue, newValue) -> {
            selectedNote.setContent(newValue);

            Label label = (Label) selectedPostIt.getChildren().get(0);
            label.setText(newValue);
        }));

        descriptionField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            noteDao.update(this.selectedNote);
        });
    }

    private void setReminderTimeFields() {
        LocalDate noteReminderDate = selectedNote.getReminderTime()
                .map(LocalDateTime::toLocalDate)
                .orElse(null);

        LocalTime noteReminderTime = selectedNote.getReminderTime()
                .map(LocalDateTime::toLocalTime)
                .orElse(null);

        reminderDate.setValue(noteReminderDate);
        reminderTime.setValue(noteReminderTime);

        setReminderDateField();
        setReminderTimeField();
    }

    private void setReminderDateField() {
        reminderDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            LocalTime localTime = selectedNote.getReminderTime()
                    .map(LocalDateTime::toLocalTime)
                    .orElse(LocalTime.now());

            LocalDateTime newReminderDate = LocalDateTime.of(newValue, localTime);
            selectedNote.setReminderTime(newReminderDate);
        });

        reminderDate.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            noteDao.update(selectedNote);
        });
    }

    private void setReminderTimeField() {
        reminderTime.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            LocalDate localDate = selectedNote.getReminderTime()
                    .map(LocalDateTime::toLocalDate)
                    .orElse(LocalDate.now());

            LocalDateTime newReminderTime = LocalDateTime.of(localDate, newValue);
            selectedNote.setReminderTime(newReminderTime);
        });

        reminderTime.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            noteDao.update(selectedNote);
        });
    }

    private void styleText(Label bodyText) {
        bodyText.setWrapText(true);
        VBox.setVgrow(bodyText, Priority.ALWAYS);
        bodyText.getStyleClass().addAll("normal-text");
    }

    private void stylePostIt(Note addedNote, VBox postIt) {
        JFXDepthManager.setDepth(postIt, 1);
        postIt.setMinSize(215, 130);
        postIt.setPrefSize(215, 130);
        postIt.setMaxSize(215, 130);

        postIt.setPadding(new Insets(15, 15, 15, 15));

        postIt.getStyleClass().addAll("form");
        postIt.setStyle("-fx-background-color: " + categoryColorOfNote(addedNote));
        postIt.setAlignment(Pos.CENTER_LEFT);

        setHighlightOnHover(addedNote, postIt);
    }

    private void setHighlightOnHover(Note addedNote, VBox postIt) {
        postIt.setOnMouseEntered(event -> {
            postIt.setStyle("-fx-background-color: " + ChiclePadApp.darken(categoryColorOfNote(addedNote), 0.95));
        });

        postIt.setOnMouseExited(event -> {
            postIt.setStyle("-fx-background-color: " + categoryColorOfNote(addedNote));
        });
    }

    public Note deleteSelected() {
        layout.getChildren().removeIf(child -> child == selectedPostIt);
        notes.remove(selectedNote);
        return selectedNote;
    }

    public void clearNotes() {
        layout.getChildren().clear();
    }

    public void setNewFilter(String filter) {
        this.filter = filter;

        notes.stream()
                .filter(note -> fitsFilter(note, filter))
                .forEach(this::addNoteToLayout);
    }

    private boolean fitsFilter(Note note, String filter) {
        return note.getContent().contains(filter) ||
                note.getReminderTime().map(time -> time.toString().contains(filter)).orElse(false);
    }

    private String categoryColorOfNote(Note note) {
        if (!note.getCategories().isEmpty()) {
            return note.getCategories().get(0).getColor();
        } else {
            return ChiclePadColor.toHex(ChiclePadColor.WHITE);
        }
    }

    @Override
    public void filterByCategory(List<Category> categories) {

    }

    @Override
    public void setCategoryToSelectedEntry(Category category) {

    }
}
