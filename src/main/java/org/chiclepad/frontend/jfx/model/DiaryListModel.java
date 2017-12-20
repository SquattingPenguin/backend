package org.chiclepad.frontend.jfx.model;

import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.effects.JFXDepthManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chiclepad.backend.Dao.DaoFactory;
import org.chiclepad.backend.Dao.DiaryPageDao;
import org.chiclepad.backend.entity.Category;
import org.chiclepad.backend.entity.DiaryPage;
import org.chiclepad.constants.ChiclePadColor;
import org.chiclepad.frontend.jfx.ChiclePadApp;

import java.util.ArrayList;
import java.util.List;

public class DiaryListModel implements ListModel {

    private List<DiaryPage> diaryPages;

    private VBox layout;

    private VBox selectedDiaryPageLine;

    private DiaryPage selectedDiaryPage;

    private DiaryPageDao diaryPageDao;

    private String textFilter = "";

    private List<Category> categoriesFilter = new ArrayList<>();

    private boolean clearedScene;

    public DiaryListModel(VBox layout) {
        this.layout = layout;
        this.diaryPageDao = DaoFactory.INSTANCE.getDiaryPageDao();
        this.diaryPages = new ArrayList<>();
    }

    public void add(DiaryPage diaryPage) {
        diaryPages.add(diaryPage);
        addDiaryPageToLayout(diaryPage);
    }

    private void addDiaryPageToLayout(DiaryPage diaryPage) {
        VBox diaryPageLine = new VBox();
        styleDiaryPageLine(diaryPageLine, diaryPage);
        setHighlightOnHover(diaryPageLine, diaryPage);
        diaryPageLine.addEventFilter(MouseEvent.MOUSE_PRESSED, this.selectDiaryPage(diaryPageLine, diaryPage));

        HBox headline = new HBox();
        headline.setSpacing(10);

        Label dateLabel = createDateLabel(diaryPage);

        FontAwesomeIcon dropdownIcon = new FontAwesomeIcon();
        styleDropdownIcon(dropdownIcon);

        setHeadlineCallbacks(diaryPage, diaryPageLine, headline, dropdownIcon);
        headline.getChildren().addAll(dateLabel, dropdownIcon);

        diaryPageLine.getChildren().add(headline);

        layout.getChildren().add(diaryPageLine);
    }

    private void setHeadlineCallbacks(
            DiaryPage diaryPage,
            VBox diaryPageLine,
            HBox headline,
            FontAwesomeIcon dropdownIcon
    ) {
        headline.setOnMousePressed(event -> {
            if (isExpanded(diaryPageLine)) {
                collapse(diaryPageLine);
            } else {
                expand(diaryPage, diaryPageLine);
            }
        });

        headline.setOnMouseEntered(event -> dropdownIcon.setFill(ChiclePadColor.PRIMARY));
        headline.setOnMouseExited(event -> dropdownIcon.setFill(ChiclePadColor.BLACK));
    }

    private void collapse(VBox diaryPageLine) {
        diaryPageLine.getChildren().remove(1);
    }

    private void expand(DiaryPage diaryPage, VBox diaryPageLine) {
        JFXTextArea text = new JFXTextArea(diaryPage.getText());
        setDiaryPageTextCallbacks(diaryPage, text);
        styleDiaryPageTextArea(text);
        diaryPageLine.getChildren().add(text);
    }

    private void setDiaryPageTextCallbacks(DiaryPage diaryPage, JFXTextArea text) {
        text.textProperty().addListener(((observable, oldValue, newValue) -> {
            diaryPage.setText(newValue);
        }));

        text.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue) {
                return;
            }

            this.diaryPageDao.update(diaryPage);
        }));
    }

    private void styleDiaryPageTextArea(JFXTextArea text) {
        text.getStyleClass().add("small-normal-text");
        text.setFocusColor(ChiclePadColor.PRIMARY);
        text.setUnFocusColor(ChiclePadColor.GREY_TEXT);

        HBox.setHgrow(text, Priority.ALWAYS);

        int rows = Math.max(2, Math.min(5, text.getText().length() / 100));
        text.setPrefRowCount(rows);
    }

    private void styleDiaryPageLine(VBox diaryPageLine, DiaryPage diaryPage) {
        diaryPageLine.getStyleClass().add("form");
        diaryPageLine.setStyle("-fx-background-color: " + categoryColorOfDiaryPage(diaryPage));

        diaryPageLine.setPadding(new Insets(15, 15, 15, 15));
        JFXDepthManager.setDepth(diaryPageLine, 1);
    }

    private void setHighlightOnHover(VBox diaryPageLine, DiaryPage diaryPage) {
        diaryPageLine.setOnMouseEntered(event -> {
            diaryPageLine.setStyle("-fx-background-color: " + ChiclePadApp.darken(categoryColorOfDiaryPage(diaryPage), 0.95));
        });

        diaryPageLine.setOnMouseExited(event -> {
            diaryPageLine.setStyle("-fx-background-color: " + categoryColorOfDiaryPage(diaryPage));
        });
    }

    private void styleDropdownIcon(FontAwesomeIcon dropdownIcon) {
        dropdownIcon.setIcon(FontAwesomeIconName.CARET_DOWN);
        dropdownIcon.setSize("1.75em");
    }

    private boolean isExpanded(VBox diaryPageLine) {
        return diaryPageLine.getChildren().size() > 1;
    }

    private Label createDateLabel(DiaryPage diaryPage) {
        Label dateLabel = new Label(diaryPage.getRecordedDay().toString());
        dateLabel.getStyleClass().add("normal-text");
        return dateLabel;
    }

    private EventHandler<MouseEvent> selectDiaryPage(VBox selectedDiaryPageLine, DiaryPage selectedDiaryPage) {
        return event -> {
            this.deselectPreviousDiaryPage();
            setSelectedDiaryPage(selectedDiaryPageLine, selectedDiaryPage);
            JFXDepthManager.setDepth(selectedDiaryPageLine, 2);
            this.selectedDiaryPageLine.getStyleClass().add("bordered");
        };
    }

    private void setSelectedDiaryPage(VBox selectedDiaryPageLine, DiaryPage selectedDiaryPage) {
        this.selectedDiaryPageLine = selectedDiaryPageLine;
        this.selectedDiaryPage = selectedDiaryPage;
    }

    private void deselectPreviousDiaryPage() {
        if (selectedDiaryPageLine == null || selectedDiaryPage == null) {
            return;
        }
        selectedDiaryPageLine.getStyleClass().removeIf(cssClass -> cssClass.equals("bordered"));
        JFXDepthManager.setDepth(selectedDiaryPageLine, 1);
    }

    public void clearDiaryPages() {
        layout.getChildren().clear();
        this.clearedScene = true;
    }

    public DiaryPage deleteSelected() {
        layout.getChildren().removeIf(child -> child == selectedDiaryPageLine);
        diaryPages.remove(selectedDiaryPage);
        return selectedDiaryPage;
    }

    @Override
    public void setCategoryToSelectedEntry(Category category) {
        this.selectedDiaryPage.getCategories().forEach(unboundCategory -> {
            this.diaryPageDao.unbind(unboundCategory, this.selectedDiaryPage);
        });
        this.selectedDiaryPage.getCategories().clear();
        this.selectedDiaryPage.getCategories().add(category);
        this.diaryPageDao.bind(category, this.selectedDiaryPage);
        this.selectedDiaryPageLine.setStyle("-fx-background-color: " + categoryColorOfDiaryPage(this.selectedDiaryPage));
    }

    @Override
    public void clearEntries() {
        this.clearDiaryPages();
    }

    @Override
    public void filterByCategory(List<Category> categories) {
        this.categoriesFilter = categories;
        this.filter();
    }

    public void filterByText(String textFilter) {
        this.textFilter = textFilter;
        this.filter();
    }

    private boolean fitsTextFilter(DiaryPage diaryPage) {
        return diaryPage.getText().contains(this.textFilter) ||
                diaryPage.getRecordedDay().toString().contains(this.textFilter);
    }

    private void filter() {
        if (clearedScene) {
            diaryPages.stream()
                    .filter(this::fitsTextFilter)
                    .filter(diaryPage -> fitsCategoryFilter(diaryPage, this.categoriesFilter))
                    .forEach(this::addDiaryPageToLayout);

            this.clearedScene = false;
        }
    }

    private String categoryColorOfDiaryPage(DiaryPage diaryPage) {
        if (!diaryPage.getCategories().isEmpty()) {
            return diaryPage.getCategories().get(0).getColor();
        } else {
            return ChiclePadColor.toHex(ChiclePadColor.WHITE);
        }
    }

    @Override
    public void deleteCategoriesForEntry() {
        System.out.println("Deleting category");
        this.selectedDiaryPage.getCategories().forEach(category -> this.diaryPageDao.unbind(category, this.selectedDiaryPage));
        System.out.println("Category deleted");
        this.selectedDiaryPage.getCategories().clear();
        this.selectedDiaryPageLine.setStyle("-fx-background-color: " + categoryColorOfDiaryPage(this.selectedDiaryPage));
    }

}
