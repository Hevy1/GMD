package controller;

import Meddra.MeddraIndications;
import Utils.UtilsFonctions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import model.Indication;
import model.SideEffect;

import static Utils.UtilsFonctions.AllMedraWhichCauseSe;
import static Utils.UtilsFonctions.AllMedraWhichHealSe;

/**
 * Controlleur de l'interface
 */
public class MainController {

    //Onglets
    @FXML
    TabPane tabPane;

    // Bar de recherche
    @FXML
    private TextField searchBar;


    @FXML
    private ProgressIndicator progressBar;


    @FXML private Label mappingQualitySE;
    @FXML private Label mappingQualityIndic;
    @FXML private Label mappingQualityMaladie;

    @FXML private Label nbResMaladie;
    @FXML private Label nbResSE;
    @FXML private Label nbResIndic;

    @FXML
    private ScrollPane maladieScrollPane;
    @FXML
    private Label maladiesLabel;

    @FXML
    private TableView<SideEffect> MedicSoinTable;
    @FXML private TableColumn<SideEffect, String> idMedic;
    @FXML private TableColumn<SideEffect, String> sideEffect1;
    @FXML private TableColumn<SideEffect, String> sideEffect2;
    @FXML private TableColumn<SideEffect, String> sourceSoignant;

    @FXML
    private TableView<Indication> MedicCauseTable;
    @FXML private TableColumn<Indication, String> medic;
    @FXML private TableColumn<Indication, String> synonyme;
    @FXML private TableColumn<Indication, String> symptome;
    @FXML private TableColumn<Indication, String> proba;
    @FXML private TableColumn<Indication, String> sourceCause;

    /**
     * Fonction appelée au chargement de la vue. Défini le comportement des objets de la vue.
     */
    @FXML
    public void initialize(){
        progressBar.setVisible(false);
        searchBar.setPromptText("Abdominal pain");
        /*searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            updateSearch(newValue);
        });*/

        searchBar.setOnKeyPressed( event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                updateSearch(searchBar.getText());
            }
        } );

        tabPane.getSelectionModel().select(2);

        maladieScrollPane.setFitToWidth(true);

        idMedic.setCellValueFactory(new PropertyValueFactory<SideEffect, String>("idMedic"));
        sideEffect1.setCellValueFactory(new PropertyValueFactory<SideEffect, String>("sideEffect1"));
        sideEffect2.setCellValueFactory(new PropertyValueFactory<SideEffect, String>("sideEffect2"));
        sourceSoignant.setCellValueFactory(new PropertyValueFactory<SideEffect, String>("sourceSoignant"));

        MedicSoinTable.getColumns().setAll(idMedic, sideEffect1, sideEffect2, sourceSoignant);

        medic.setCellValueFactory(new PropertyValueFactory<Indication, String>("medic"));
        symptome.setCellValueFactory(new PropertyValueFactory<Indication, String>("symptome"));
        proba.setCellValueFactory(new PropertyValueFactory<Indication, String>("proba"));
        sourceCause.setCellValueFactory(new PropertyValueFactory<Indication, String>("sourceCause"));

        MedicCauseTable.getColumns().setAll(medic, symptome, proba,sourceCause );
    }

    public void updateSearch(String search)
    {
        progressBar.setVisible(true);
        new Thread(){
            public void run() {

                try {
                    Platform.runLater(() -> progressBar.setProgress( 0 ));
                    ArrayList<ArrayList<String>> sideEffectResult = AllMedraWhichHealSe(search);
                    Platform.runLater(() -> fillSideEffectTableView(sideEffectResult));
                    Platform.runLater(() -> progressBar.setProgress( 0.33 ));
                    Thread.sleep(1000);

                    ArrayList<ArrayList<String>> indicationResult = AllMedraWhichCauseSe(search);
                    Platform.runLater(() -> fillIndicationTableView(indicationResult));

                    Platform.runLater(() -> progressBar.setProgress( 0.66 ));
                    Thread.sleep(1000);

                    List<List<String>> diseaseResult =  UtilsFonctions.getDisease(search);
                    Platform.runLater(() -> fillDiseaseScrollPane(diseaseResult));

                    Platform.runLater(() -> progressBar.setProgress( 1 ));

                    Thread.sleep(10000);

                    progressBar.setVisible(false);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void fillSideEffectTableView(ArrayList<ArrayList<String>> result){

        int sourceColumnSize = 150;

        MedicSoinTable.getItems().clear();

        for (ArrayList<String> line : result) {

            MedicSoinTable.getItems().add(new SideEffect(line.get(0), line.get(1), line.get(2), line.get(3)));
        }

        mappingQualityIndic.setText(String.valueOf(UtilsFonctions.MappingQualityIndication()));
        nbResIndic.setText(String.valueOf(result.size()));
    }

    public void fillIndicationTableView(ArrayList<ArrayList<String>> result){

        MedicCauseTable.getItems().clear();

        for (ArrayList<String> line : result) {
            MedicCauseTable.getItems().add(new Indication(line.get(0), line.get(2), line.get(3), line.get(4)));
        }


        mappingQualitySE.setText(String.valueOf(UtilsFonctions.MappingQualitySeFreq()));
        nbResSE.setText(String.valueOf(result.size()));
    }

    public void fillDiseaseScrollPane(List<List<String>> result){

        maladiesLabel.setText("");

        StringBuilder sb = new StringBuilder();
        for (List<String> line : result) {
            sb.append("Name : " + line.get(0));
            sb.append("\n\tSymptoms : " + line.get(3));

            for (int i = 4; i<line.size(); i++) {
                sb.append("; " + line.get(i));
            }

            sb.append("\n\tNote : " + line.get(2));
            sb.append("\n\tSource : " + line.get(1)+"\n\n");

            maladiesLabel.setText(maladiesLabel.getText() + sb);
            maladiesLabel.setWrapText(true);
            sb.setLength(0);
        }

        mappingQualityMaladie.setText("100%");
        nbResMaladie.setText(String.valueOf(result.size()));
    }
}
