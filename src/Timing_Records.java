import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Timing_Records {

    private Stage stage;
    private Label allTimeTotalLabel = new Label("Total Earnings: €0.00");
    private Label totalHoursLabel = new Label("Total Hours: 0.00");
    private Label calculateSelectedDateSalary;

    private TextField txtRate, txt_search;
    private DatePicker datePicker;
    private DatePicker startDate;
    private DatePicker endDate;
    private Spinner<Integer> startHour, startMinute, endHour, endMinute;
    private Label totalPreview;
    private Button saveBtn;

    private LocalTime startTime, endTime;
    private float calculatedHours = 0f, calculatedEarning = 0f;

    private final TableView<records> recordTable = new TableView<>();
    private final ObservableList<records> dataList = FXCollections.observableArrayList();
    private XYChart.Series<Number, Number> series = new XYChart.Series<>();

    public Timing_Records(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        databaseConnection.setupDatabase();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setPadding(new Insets(20));

        root.setTop(createHeader());
        root.setLeft(createControlPanel());
        root.setCenter(createRightPanel());
        root.setBottom(createChartPanel());

        Scene scene = new Scene(root, 1280, 1100);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("timing.css")).toExternalForm());

        stage.setScene(scene);
        stage.setTitle("⏰ Time Management Dashboard");
        stage.show();

        calculateLastSalaryRate();
        setupListeners();
        loadData();
        refreshTotals();
    }

    private HBox createHeader() {
        HBox header = new HBox(50);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        Label title = new Label("⏰ TIME MANAGEMENT");
        title.setFont(Font.font("Arial", 32));
        title.getStyleClass().add("title-label");

        VBox totals = new VBox(5, allTimeTotalLabel, totalHoursLabel);
        totals.getStyleClass().add("total-preview");

        txt_search = new TextField();
        txt_search.setPromptText("Search by Date (dd.MM.yy)");
        txt_search.getStyleClass().add("input-field");
        txt_search.setMinWidth(300);
        txt_search.textProperty().addListener((obs, oldValue, newValue) -> {
            searchWorkDaysAsync(newValue);
        });


        header.getChildren().addAll(title, totals, txt_search);
        return header;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(400);
        panel.setPadding(new Insets(15));
        panel.getStyleClass().add("control-panel");


        // ===== ENTRY PANE =====
        Label rateLbl = new Label("€ Hourly Rate");
        rateLbl.getStyleClass().add("field-label");
        txtRate = new TextField();
        txtRate.setPromptText("11.00");
        txtRate.getStyleClass().add("input-field");

        Label dateLbl = new Label("📅 Work Date");
        dateLbl.getStyleClass().add("field-label");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("input-field");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(dateFormatter) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dateFormatter) : null;
            }
        });
        datePicker.setPromptText("dd.MM.yy");

        Label sLbl = new Label("⏳ Start Time");
        sLbl.getStyleClass().add("field-label");
        startHour = new Spinner<>(0, 23, 9);
        startMinute = new Spinner<>(0, 59, 0);
        stylizeSpinner(startHour);
        stylizeSpinner(startMinute);
        HBox sBox = new HBox(10, startHour, startMinute);
        sBox.setAlignment(Pos.CENTER_LEFT);

        Label eLbl = new Label("⏳ End Time");
        eLbl.getStyleClass().add("field-label");
        endHour = new Spinner<>(0, 23, 17);
        endMinute = new Spinner<>(0, 59, 0);
        stylizeSpinner(endHour);
        stylizeSpinner(endMinute);
        HBox eBox = new HBox(10, endHour, endMinute);
        eBox.setAlignment(Pos.CENTER_LEFT);

        totalPreview = new Label("0.00 = €0.00");
        totalPreview.getStyleClass().add("total-preview");

        saveBtn = new Button("✔ Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            if (calculatedHours > 0) {
                save();
            } else showAlert(Alert.AlertType.ERROR, "Error", "End Time must be after Start Time.");
        });

        VBox entryPane = new VBox(12, rateLbl, txtRate, dateLbl, datePicker, sLbl, sBox, eLbl, eBox, totalPreview, saveBtn);
        entryPane.setPadding(new Insets(10));
        entryPane.setStyle("-fx-border-color: #aaa; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-radius: 8; -fx-background-color: rgba(255,255,255,0.05);");

        // CALCULATION PANE
        Label calcTitle = new Label("📅 Earning Calculation");
        calcTitle.setFont(Font.font("Arial", 16));
        calcTitle.getStyleClass().add("field-label");

        startDate = new DatePicker(LocalDate.now());
        startDate.getStyleClass().add("input-field");
        startDate.setPromptText("Start Date (dd.MM.yy)");

        endDate = new DatePicker(LocalDate.now());
        endDate.getStyleClass().add("input-field");
        endDate.setPromptText("End Date (dd.MM.yy)");

        Button calculate = new Button("\uD83E\uDDEE Calculate");
        calculate.getStyleClass().add("btn-primary");
        calculate.setOnMouseClicked(event -> calculateMonthlyIncome());

        calculateSelectedDateSalary = new Label("Earning Calculation: €0.00");
        calculateSelectedDateSalary.getStyleClass().add("total-preview");

        VBox calculationPane = new VBox(10, calcTitle, startDate, endDate, calculate, calculateSelectedDateSalary);
        calculationPane.setPadding(new Insets(10));
        calculationPane.setStyle("-fx-border-color: #66ccff; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-radius: 8; -fx-background-color: rgba(102,204,255,0.1);");

        panel.getChildren().addAll(entryPane, calculationPane);
        return panel;
    }

    private VBox createRightPanel() {
        VBox right = new VBox(10);
        right.setPrefWidth(860);

        recordTable.setPrefHeight(1000);
        recordTable.getColumns().clear();

        TableColumn<records, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<records, java.sql.Date> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(java.sql.Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toLocalDate().format(dateFormatter));
            }
        });
        dateCol.setPrefWidth(140);

        TableColumn<records, LocalTime> sCol = new TableColumn<>("Start");
        sCol.setCellValueFactory(new PropertyValueFactory<>("startHour"));
        sCol.setPrefWidth(120);

        TableColumn<records, LocalTime> eCol = new TableColumn<>("End");
        eCol.setCellValueFactory(new PropertyValueFactory<>("endHour"));
        eCol.setPrefWidth(120);

        TableColumn<records, Float> tCol = new TableColumn<>("Hours");
        tCol.setCellValueFactory(new PropertyValueFactory<>("formattedTotal"));
        tCol.setPrefWidth(100);

        TableColumn<records, Float> earnCol = new TableColumn<>("Earnings (€)");
        earnCol.setCellValueFactory(new PropertyValueFactory<>("formattedEarning"));
        earnCol.setPrefWidth(160);

        recordTable.getColumns().addAll(idCol, dateCol, sCol, eCol, tCol, earnCol);
        recordTable.setItems(dataList);
        recordTable.getStyleClass().add("record-table");

        recordTable.setRowFactory(tv -> {
            TableRow<records> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem delete = new MenuItem("❌ DELETE");
            delete.setOnAction(ev -> {
                if (row.getItem() != null) {
                    deleteRecord(row.getItem());
                }
            });
            menu.getItems().add(delete);
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    menu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        right.getChildren().add(recordTable);
        return right;
    }

    private LineChart<Number, Number> createChartPanel() {
        NumberAxis x = new NumberAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("Entry #");
        y.setLabel("Earnings (€)");

        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.setTitle("💰 Earnings Over Time");
        chart.getStyleClass().add("glass-chart");
        chart.setCreateSymbols(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);

        series = new XYChart.Series<>();
        chart.getData().add(series);
        chart.setPrefHeight(280);

        return chart;
    }

    private void stylizeSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.getEditor().getStyleClass().add("input-field");
        spinner.setPrefSize(100, 30);
    }

    private void setupListeners() {
        startHour.valueProperty().addListener((o, oldV, newV) -> computePreview());
        startMinute.valueProperty().addListener((o, oldV, newV) -> computePreview());
        endHour.valueProperty().addListener((o, oldV, newV) -> computePreview());
        endMinute.valueProperty().addListener((o, oldV, newV) -> computePreview());
        txtRate.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d{0,2})?")) txtRate.setText(oldVal);
            computePreview();
        });
    }


    // search functionality
    private void searchWorkDaysAsync(String keyword){
        new Thread(() -> {
            ObservableList<records> searchResults = FXCollections.observableArrayList();
            String query = "SELECT * FROM work_time WHERE date LIKE ? ORDER BY id DESC";
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, "%" + keyword + "%");
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    searchResults.add(new records(rs.getInt("id"), rs.getDate("date"), rs.getObject("start_time", LocalTime.class),
                            rs.getObject("end_time", LocalTime.class), rs.getFloat("total"), rs.getFloat("earning")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            javafx.application.Platform.runLater(() -> dataList.setAll(searchResults));
        }).start();
    }

    private void computePreview() {
        try {
            startTime = LocalTime.of(startHour.getValue(), startMinute.getValue());
            endTime = LocalTime.of(endHour.getValue(), endMinute.getValue());
            calculatedHours = endHour.getValue() + endMinute.getValue() / 60f - (startHour.getValue() + startMinute.getValue() / 60f);
            if (calculatedHours < 0) {
                calculatedHours = 0;
                calculatedEarning = 0;
                totalPreview.setText("0.00 = €0.00");
                return;
            }
            float rate = txtRate.getText().isEmpty() ? 0 : Float.parseFloat(txtRate.getText());
            calculatedEarning = calculatedHours * rate;
            totalPreview.setText(String.format("%.2f = €%.2f", calculatedHours, calculatedEarning));
        } catch (Exception ignored) {}
    }

    private void animateSave() {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), saveBtn);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.05); st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    private void save() {
        new Thread(() -> {
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("INSERT INTO work_time(date,start_time,end_time,total,earning) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                pst.setDate(1, java.sql.Date.valueOf(datePicker.getValue()));
                pst.setObject(2, startTime);
                pst.setObject(3, endTime);
                pst.setFloat(4, calculatedHours);
                pst.setFloat(5, calculatedEarning);
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    ResultSet keys = pst.getGeneratedKeys();
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        records newRec = new records(id, java.sql.Date.valueOf(datePicker.getValue()), startTime, endTime, calculatedHours, calculatedEarning);
                        javafx.application.Platform.runLater(() -> {
                            dataList.add(0, newRec);
                            series.getData().add(new XYChart.Data<>(series.getData().size() + 1, newRec.getDailyEarning()));
                            refreshTotals();
                            animateSave();
                        });
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void deleteRecord(records r) {
        new Thread(() -> {
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("DELETE FROM work_time WHERE id=?")) {
                pst.setInt(1, r.getId());
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    javafx.application.Platform.runLater(() -> {
                        dataList.remove(r);
                        refreshTotals();
                        series.getData().clear();
                        int idx = 1;
                        for (records rec : dataList) {
                            series.getData().add(new XYChart.Data<>(idx++, rec.getDailyEarning()));
                        }
                    });
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void loadData() {
        new Thread(() -> {
            ObservableList<records> list = FXCollections.observableArrayList();
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("SELECT * FROM work_time ORDER BY id DESC");
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new records(rs.getInt("id"), rs.getDate("date"), rs.getObject("start_time", LocalTime.class),
                            rs.getObject("end_time", LocalTime.class), rs.getFloat("total"), rs.getFloat("earning")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            javafx.application.Platform.runLater(() -> {
                dataList.setAll(list);
                populateChart();
            });
        }).start();
    }

    private void populateChart() {
        series.getData().clear();
        int idx = 1;
        for (records r : dataList) {
            series.getData().add(new XYChart.Data<>(idx++, r.getDailyEarning()));
        }
    }

    private void refreshTotals() {
        new Thread(() -> {
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("SELECT TRUNCATE(SUM(earning),2) AS total_earning, SUM(total) AS totalHours FROM work_time");
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    float totalEarn = rs.getFloat("total_earning");
                    float totalHours = rs.getFloat("totalHours");
                    javafx.application.Platform.runLater(() -> {
                        allTimeTotalLabel.setText(String.format("Total Earnings: €%.2f", totalEarn));
                        totalHoursLabel.setText(String.format("Total Hours: %.2f", totalHours));
                    });
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void calculateMonthlyIncome() {
        new Thread(() -> {
            String query = "select sum(earning) as calculation from work_time where date between ? and ?";
            LocalDate stdate = startDate.getValue();
            LocalDate endate = endDate.getValue();
            if (stdate == null || endate == null) return;
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setDate(1, java.sql.Date.valueOf(stdate));
                pst.setDate(2, java.sql.Date.valueOf(endate));
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    float calc = rs.getFloat("calculation");
                    javafx.application.Platform.runLater(() -> calculateSelectedDateSalary.setText(String.format("Calculation: %.2f", calc)));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void calculateLastSalaryRate() {
        new Thread(() -> {
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement("SELECT earning/total AS rate FROM work_time ORDER BY id DESC LIMIT 1");
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    float rate = rs.getFloat("rate");
                    javafx.application.Platform.runLater(() -> txtRate.setText(String.format("%.2f", rate)));
                }
            } catch (SQLException ignored) {}
        }).start();
    }

    private void showAlert(Alert.AlertType t, String title, String message) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
