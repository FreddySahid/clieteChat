package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Optional;
import java.util.StringTokenizer;

public class Main extends Application {
    private boolean conectar= false;
    private Socket conexion;
    private BufferedReader lector;
    private BufferedWriter escritor;
    private TextField txtServidos, txtPuerto, txtApodo;
    private TextField txtMensaje;
    private TextArea txtMensajes;
    private Button btnConectar, btnEnviarMensaje;
    private HiloEscuchaMensajes hilo;
    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();
        Label lblServidor, lblPuerto, lblApodo;
        TitledPane panelConexion, panelMensajes, panelMensaje;

        HBox contenedorConexion = new HBox();
        contenedorConexion.setSpacing(10);
        contenedorConexion.setPadding(new Insets(10));
        lblServidor = new Label( "Servidor");
        lblServidor.setMinHeight(25);
        lblApodo = new Label( "Apodo");
        lblApodo.setMinHeight(25);
        lblPuerto = new Label( "Puerto");
        lblPuerto.setMinHeight(25);
        txtServidos = new TextField("localhost");
        txtPuerto = new TextField("4500");
        txtPuerto.setMaxWidth(50);
        txtApodo = new TextField();
        btnConectar = new Button("Conectar");
        btnConectar.setOnAction(evt -> {
            if (!conectar) {
                String apodo = txtApodo.getText();
                if (apodo.trim().length() > 0)
                    conectarServidor(txtServidos.getText(), Integer.parseInt(txtPuerto.getText()),
                            txtApodo.getText());
                else {
                    Alert DialogoError = new Alert(Alert.AlertType.ERROR);
                    DialogoError.setTitle("Error");
                    DialogoError.setHeaderText("No pusiste tu nombre");
                    DialogoError.setContentText("Pon tu nombre");
                    DialogoError.showAndWait();
                }
            }else{
                desconectarServidor();
            }
        });
        contenedorConexion.getChildren().addAll(lblServidor,txtServidos,lblPuerto,
                txtPuerto, lblApodo, txtApodo, btnConectar);
        panelConexion = new TitledPane();
        panelConexion.setText("Datos de conexión");
        panelConexion.setContent(contenedorConexion);
        txtMensajes = new TextArea();
        txtMensajes.setMinHeight(300);

        panelMensajes = new TitledPane();
        panelMensajes.setText("Mensajes");
        panelMensajes.setContent(txtMensajes);

        HBox contenedorMensaje = new HBox();
        contenedorMensaje.setSpacing(10);
        contenedorMensaje.setPadding(new Insets(10));
        txtMensaje = new TextField();
        txtMensaje.setPromptText("Escriba el mensaje a enviar");
        txtMensaje.setMinWidth(400);
        btnEnviarMensaje = new Button("Enviar");
        btnEnviarMensaje.setOnAction(evt -> {
            enviarMensaje(txtMensaje.getText());
            txtMensaje.setText(" ");
            txtMensaje.requestFocus();
        });
        contenedorMensaje.getChildren().addAll(txtMensaje, btnEnviarMensaje);
        panelMensaje = new TitledPane();
        panelMensaje.setText("Enviar mensaje");
        panelMensaje.setContent(contenedorMensaje);
        root.setTop(panelConexion);
        root.setCenter(panelMensajes);
        root.setBottom(panelMensaje);

        habilitarConexion(true);
        habilitarEnvioMensaje(false);

        primaryStage.setTitle("Cliente chat");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();

       // hilo = new HiloEscuchaMensajes();
        //hilo.start();
        TareaEscuchaMEnsajes tarea = new TareaEscuchaMEnsajes();
        txtMensajes.textProperty().bind(tarea.messageProperty());
        new Thread(tarea).start();
    }

    private void habilitarConexion(boolean hab){
        txtServidos.setDisable(!hab);
        txtPuerto.setDisable(!hab);
        txtApodo.setDisable(!hab);
        if(hab)
        btnConectar.setText("Conectar");
        else
            btnConectar.setText("Desconectar");
    }

    private void habilitarEnvioMensaje(boolean hab){
        txtMensaje.setDisable(!hab);
        btnEnviarMensaje.setDisable(!hab);
    }

    public void conectarServidor(String servidor, int puerto, String apodo){
        try {
            conexion = new Socket(servidor, puerto);
            lector = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
            escritor = new BufferedWriter(new OutputStreamWriter(conexion.getOutputStream()));
            if(enviarMensaje(apodo)){
                conectar = true;
                habilitarConexion(false);
                habilitarEnvioMensaje(true);
            }else{
                throw new IOException("");
            }
        } catch (Exception e){
            Alert DialogoError = new Alert(Alert.AlertType.ERROR);
            DialogoError.setTitle("Error");
            DialogoError.setHeaderText("Problema de conexión");
            DialogoError.setContentText("Problema al conectar servidor");
            DialogoError.showAndWait();
        }
    }

    private void desconectarServidor(){
        hilo.stop();
        hilo = null;
        try {
            enviarMensaje("**SALIR**");
            lector.close();
            escritor.close();
            conexion.close();
        }catch (Exception e){

        }
        conectar = false;
        habilitarConexion(true);
        habilitarEnvioMensaje(false);
        txtMensaje.setText("");

    }

    public boolean enviarMensaje(String mensaje){
        try {
            escritor.write(mensaje+"\r\n");
            escritor.flush();
            return true;
        }catch (IOException e){
            return false;
        }
    }

    class TareaEscuchaMEnsajes extends Task<String>{
        private String mensajes = "";
        @Override
        protected String call()/* throws Exception */ {
            while(true){
                try{
                    String mensajet = lector.readLine();
                    StringTokenizer st = new StringTokenizer(mensajet, ",");

                    String id = st.nextToken();
                    String mensaje = st.nextToken();
                    if(mensaje.startsWith("+ OK ") || mensaje.startsWith("+ REQ "))
                        continue;
                    mensajes = mensajes + mensaje+"\n";
                    updateMessage(mensajes);
                }catch (Exception e){

                }
            }
        }
    }
    class HiloEscuchaMensajes extends Thread{
        public void run(){
            while(true){
                try{
                    String mensaje = lector.readLine();
                    if(mensaje.startsWith("+ OK ") || mensaje.startsWith("+ REQ "))
                        continue;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            txtMensajes.appendText(mensaje+"\n");

                        }
                    });
                }catch (Exception e){

                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
