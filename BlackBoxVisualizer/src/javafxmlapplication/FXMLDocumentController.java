package javafxmlapplication;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Scanner;
import javafx.event.ActionEvent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FXMLDocumentController {

    private Button btnConnectBluetooth;

    @FXML
    private Button btnLoadFile;

    @FXML
    private Label lblStatus;

    @FXML
    private LineChart<Number, Number> tempChart;

    @FXML
    private LineChart<Number, Number> accChart;

    @FXML
    private LineChart<Number, Number> presChart;

    private XYChart.Series<Number, Number> tempSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> accSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> presSeries = new XYChart.Series<>();
    @FXML
    private ComboBox<String> selectChart;
    @FXML
    private LineChart<Number, Number> velChart;
    private XYChart.Series<Number,Number> velSeries = new XYChart.Series<>();
    @FXML
    private VBox chartContainer;
    @FXML
    private LineChart<Number, Number> heightChart;
    private XYChart.Series<Number,Number> heightSeries = new XYChart.Series<>();
    @FXML
    private Button simulator;
    
    //private ArrayList<double> time, presion, temp, aX, aY, aZ, gX, gY, gZ;
    private File archivoSeleccionado;
    private ArrayList<Double> heights = new ArrayList<>();

    public void initialize() {
        tempChart.getData().add(tempSeries);
        accChart.getData().add(accSeries);
        presChart.getData().add(presSeries);
        velChart.getData().add(velSeries);
        heightChart.getData().add(heightSeries);
        
        selectChart.getItems().addAll("Temperature", "Pressure", "Acceleration", "Velocity", "Height");

        selectChart.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            tempChart.setVisible("Temperature".equals(newVal));
            presChart.setVisible("Pressure".equals(newVal));
            accChart.setVisible("Acceleration".equals(newVal));
            velChart.setVisible("Velocity".equals(newVal));
            heightChart.setVisible("Height".equals(newVal));
            
            chartContainer.getChildren().clear(); // Limpiar el contenedor

            switch (newVal) {
                case "Temperature":
                    chartContainer.getChildren().add(tempChart);
                    break;
                case "Pressure":
                    chartContainer.getChildren().add(presChart);
                    break;
                case "Acceleration":
                    chartContainer.getChildren().add(accChart);
                    break;
                case "Velocity":
                    chartContainer.getChildren().add(velChart);
                    break;
                case "Height":
                    chartContainer.getChildren().add(heightChart);
                    break;
                default:
                    break;
            }
        });
        //chartContainer.getChildren().clear();
        selectChart.getSelectionModel().selectFirst(); // Selección por defecto
        //chartContainer.getChildren().add(tempChart);
        
        simulator.setDisable(true);

        //btnConnectBluetooth.setOnAction(e -> connectBluetooth());
        btnLoadFile.setOnAction(e -> loadFromFile());
    }

    // No conseguí que funcionara la conexión Bluetooth, así que este método no se usa
    // La placa tampoco guarda datos en su RAM finalmente
    private void connectBluetooth() {
        
        try {
            // 1. Ejecutar el script de Python
            System.out.println("▶ Ejecutando script Python...");
            File script = new File("C:\\Users\\clara\\OneDrive - UPV\\Escritorio\\BIP Germany\\BIP_App\\src\\javafxmlapplication\\esp32_ble_receiver.py");
            ProcessBuilder pb = new ProcessBuilder("python", script.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Mostrar la salida del script en consola
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Python] " + line);
            }

            // Esperar a que termine
            int exitCode = process.waitFor();
            System.out.println("Script finalizado con código: " + exitCode);

            // 2. Leer el archivo CSV generado
            String csvFilePath = "esp32_ble_data.csv"; // o "rocket_log.csv"
            Path path = Paths.get(csvFilePath);
            if (!Files.exists(path)) {
                System.out.println("Archivo CSV no encontrado: " + csvFilePath);
                return;
            }

            // Leer y mostrar los datos
            File file = path.toFile();
            readFile(file);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    
    public void loadFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un archivo de texto");
        fileChooser.setInitialDirectory(new File("D:/")); //Change this directory as needed

        // Obtener el Stage actual desde cualquier nodo (por ejemplo desde un botón o TextArea)
        Stage stage = (Stage) btnLoadFile.getScene().getWindow();

        File archivo = fileChooser.showOpenDialog(stage);
        if (archivo != null) {
            archivoSeleccionado = archivo;
            System.out.println("Archivo seleccionado: " + archivoSeleccionado.getAbsolutePath());
            readFile(archivoSeleccionado);
            simulator.setDisable(false);
        }
    }

    private void readFile(File file) {
        try (Scanner sc = new Scanner(file)) {
            NumberAxis tempXAxis = (NumberAxis) tempChart.getXAxis();
            NumberAxis tempYAxis = (NumberAxis) tempChart.getYAxis();

            NumberAxis accXAxis = (NumberAxis) accChart.getXAxis();
            NumberAxis accYAxis = (NumberAxis) accChart.getYAxis();

            NumberAxis presXAxis = (NumberAxis) presChart.getXAxis();
            NumberAxis presYAxis = (NumberAxis) presChart.getYAxis();
            
            NumberAxis velXAxis = (NumberAxis) velChart.getXAxis();
            NumberAxis velYAxis = (NumberAxis) velChart.getYAxis();
            
            NumberAxis heightXAxis = (NumberAxis) heightChart.getXAxis();
            NumberAxis heightYAxis = (NumberAxis) heightChart.getYAxis();

            tempSeries.getData().clear();
            accSeries.getData().clear();
            presSeries.getData().clear();
            velSeries.getData().clear();
            heightSeries.getData().clear();
            tempChart.setCreateSymbols(false);
            accChart.setCreateSymbols(false);
            presChart.setCreateSymbols(false);
            velChart.setCreateSymbols(false);
            heightChart.setCreateSymbols(false); // si agregaste altura

            String line = sc.nextLine();
            
            int iniT = 0, endT = 0, i = 0;
            double maxV = 0, maxA = 0, maxP = 0, maxT = 0, maxH = 0, minV = 1000000000, minA = 1000000000, minP = 1000000000, minT = 1000000000, minH = 1000000000;
            double vX = 0, vY = 0, vZ = 0;
            double delta = 0.01;
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                if (line.startsWith("Millis")) continue; // Saltar encabezado
                i++;

                String[] parts = line.split("[,\\s]+"); // CSV o espacio
                if (parts.length < 9) continue;

                int time = Integer.parseInt(parts[0]);
                if(i == 1) {
                    iniT = time;
                    minP = Double.parseDouble(parts[1]);
                }
                double pres = Double.parseDouble(parts[1]);
                maxP = maxP < pres ? pres : maxP;
                minP = minP > pres ? pres : minP;
                double temp = Double.parseDouble(parts[2]);
                maxT = maxT < temp ? temp : maxT;
                minT = minT > temp ? temp : minT;
                double accX = Double.parseDouble(parts[3]);
                vX += accX * delta;
                double accY = Double.parseDouble(parts[4]);
                vY += accY * delta;
                double accZ = Double.parseDouble(parts[5]);
                vZ += accZ * delta;
                double acc = Math.sqrt(accX*accX + accY*accY + accZ*accZ);
                maxA = maxA < acc ? acc : maxA;
                minA = minA > acc ? acc : minA;
                double v = Math.sqrt(vX*vX + vY*vY + vZ*vZ);
                maxV = maxV < v ? v : maxV;
                minV = minV > v ? v : minV;
                double height = 44330 * (1.0 - Math.pow((pres/100.0)/(1013.25), (1.0/5.255)));
                heights.add(height);
                //System.out.println(height);
                maxH = maxH < height ? height : maxH;
                minH = minH > height ? height : minH;

                tempSeries.getData().add(new XYChart.Data<>(time, temp));
                accSeries.getData().add(new XYChart.Data<>(time, acc));
                presSeries.getData().add(new XYChart.Data<>(time, pres));
                velSeries.getData().add(new XYChart.Data<>(time, v));
                heightSeries.getData().add(new XYChart.Data<>(time, height));
            }
            String[] parts = line.split("[,\\s]+"); // CSV o espacio
            endT = Integer.parseInt(parts[0]);
            tempXAxis.setAutoRanging(false);
            tempXAxis.setLowerBound(iniT);
            tempXAxis.setUpperBound(endT);
            tempXAxis.setTickUnit((endT-iniT)/i + 1);
            tempYAxis.setAutoRanging(false);
            tempYAxis.setLowerBound(minT-2);     // temperatura mínima
            tempYAxis.setUpperBound(maxT+2);     // temperatura máxima
            tempYAxis.setTickUnit((maxT-minT)/i + 1);        // intervalo
            
            presXAxis.setAutoRanging(false);
            presXAxis.setLowerBound(iniT-1);
            presXAxis.setUpperBound(endT+1);
            presXAxis.setTickUnit((endT-iniT)/i + 1);
            presYAxis.setAutoRanging(false);
            presYAxis.setLowerBound(minP-2);     // temperatura mínima
            presYAxis.setUpperBound(maxP+2);     // temperatura máxima
            presYAxis.setTickUnit((maxP-minP)/i + 1);        // intervalo
            
            accXAxis.setAutoRanging(false);
            accXAxis.setLowerBound(iniT);
            accXAxis.setUpperBound(endT);
            accXAxis.setTickUnit((endT-iniT)/i + 1);
            accYAxis.setAutoRanging(false);
            accYAxis.setLowerBound(minA-2);     // temperatura mínima
            accYAxis.setUpperBound(maxA+2);     // temperatura máxima
            accYAxis.setTickUnit((maxA-minA)/i + 1);        // intervalo
            
            velXAxis.setAutoRanging(false);
            velXAxis.setLowerBound(iniT);
            velXAxis.setUpperBound(endT);
            velXAxis.setTickUnit((endT-iniT)/i + 1);
            velYAxis.setAutoRanging(false);
            velYAxis.setLowerBound(minA-2);     // temperatura mínima
            velYAxis.setUpperBound(maxA+2);     // temperatura máxima
            velYAxis.setTickUnit((maxA-minA)/i + 1);        // intervalo
            
            heightXAxis.setAutoRanging(false);
            heightXAxis.setLowerBound(iniT);
            heightXAxis.setUpperBound(endT);
            heightXAxis.setTickUnit((endT-iniT)/i + 1);
            heightYAxis.setAutoRanging(false);
            heightYAxis.setLowerBound(minH-2);     // temperatura mínima
            heightYAxis.setUpperBound(maxH+2);     // temperatura máxima
            heightYAxis.setTickUnit((maxH-minH)/i + 1);        // intervalo


            lblStatus.setText("Archivo cargado con éxito.");
        } catch (Exception e) {
            lblStatus.setText("Error al leer archivo.");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void launchSimulation(ActionEvent event) {
        try {
            File script = new File("C:\\Users\\clara\\OneDrive - UPV\\Escritorio\\BIP Germany\\BIP_App\\src\\javafxmlapplication\\simulate_rocket.py");
            ProcessBuilder pb = new ProcessBuilder("python", script.getAbsolutePath());
            //pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (Double h : heights) {
                writer.write(h.toString());
                writer.newLine();
            }
            writer.flush();
            writer.close();

            // Leer errores del proceso
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("PYTHON ERROR: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
