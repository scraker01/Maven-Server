/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.protocol.Packet;
import com.mycompany.client.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JTextArea;
import model.Model_Client;
import model.Model_Data;
import model.Model_Receive_Message;
import model.Model_Room_Setter;
import model.Model_Send_Message;
import model.Model_User_Account;

/**
 *
 * @author User
 */
public class Service {
    private static Service instance;
    private SocketIOServer server;
    private static final int PORT_NUMBER = 5556;
    private JTextArea txtArea;
    private JTextArea clientArea;
   
    
    private List<Room> listRoom;
//    private List<Model_Client> clients;
    private int curRoomIdx;
    
    
    public static Service getInstance(JTextArea txtArea, JTextArea clientArea) {
        if(instance == null) {
            
            instance = new Service(txtArea, clientArea);
        }
        return instance;
    }

    private Service(JTextArea txtArea, JTextArea clientArea) {
            this.txtArea = txtArea;
            this.clientArea = clientArea;
            this.curRoomIdx=-1;
//            clients = new ArrayList<>();
            initRoom();
    }
        
    private void initRoom(){
        ServiceRoom sr = ServiceRoom.getInstance();
        this.listRoom = sr.getAllRoom();
        
      
    }
     
    public void startServer(){
         Configuration config = new Configuration();
         config.setPort(PORT_NUMBER);
         
         server = new SocketIOServer(config);
        
         
         server.addConnectListener(new ConnectListener(){
             @Override
             public void onConnect(SocketIOClient sioc) {
                 txtArea.append("Client has connected succesfully\n");
                 clientArea.append(sioc.getRemoteAddress() +"\n");
             }
             
         });
         
         server.addEventListener("register", Model_Data.class, new DataListener<Model_Data>() {
             @Override
             public void onData(SocketIOClient sioc, Model_Data t, AckRequest ar) throws Exception {
                    ServiceUser sU = ServiceUser.getInstance();
                    boolean successRegister = sU.register(t);
                    ar.sendAckData(successRegister);
                    txtArea.append("User Registered, Username : "+ t.getUsername() +", Password : "+ t.getPass()+"\n");
             }
         });
         
         server.addEventListener("login",  Model_Data.class, new DataListener<Model_Data>() {
             @Override
             public void onData(SocketIOClient sioc, Model_Data t, AckRequest ar) throws Exception {
                   
                    ServiceUser sU = ServiceUser.getInstance();
                    Model_User_Account acc = sU.login(t);
                    
                    if(acc!=null){
                        ar.sendAckData(true, acc);
                        addClient(sioc, acc);
                       
                    }else{
                        ar.sendAckData(false);
                    }
                    
                    txtArea.append("User Login, Username : "+ t.getUsername() +", Password : "+ t.getPass()+"\n");
             }
         });
         
         //Server add listener buat room
         
         server.addEventListener("getRoomInServer", Model_Data.class, new DataListener<Model_Data>() {
            @Override
            public synchronized void onData(SocketIOClient sioc, Model_Data t, AckRequest ar) throws Exception {
                
                ServiceRoom sR = ServiceRoom.getInstance();

                List<Room> result = sR.getRoom(t);
                sioc.sendEvent("getRoom", result.toArray());

            }
         });
         
         //Server listener untuk pengambilan user dalam suatu room
         //Perlu ditentukan apakah privilege lewat event listener atau 
         server.addEventListener("getUsersInRoom", Integer.class, new DataListener<Integer>() {
             @Override
             public synchronized void onData(SocketIOClient sioc, Integer idx, AckRequest ar) throws Exception {  
              
           
                List<Model_User_Account> result = ServiceRoom.getInstance().getUsersInRoom(idx);
               
                sioc.sendEvent("getUsersInRoom", result.toArray());
             }
         });
         
         server.addEventListener("send_to_users", Model_Send_Message.class, new DataListener<Model_Send_Message>(){
             @Override
             public void onData(SocketIOClient sioc, Model_Send_Message message, AckRequest ar) throws Exception {
                 broadcastToRoom(message);
             }
    });
        
//         server.addEventListener("broadcast", eventClass, listener);
         
         server.start();
         
         txtArea.append("Connected to Server Port :"+ PORT_NUMBER +"\n");
         
    }
    public void addClient(SocketIOClient socket, Model_User_Account acc){
        int user_Id = acc.getUserId();
        List<Integer> roomList = ServiceRoom.getInstance().locateRoomForUser(user_Id);
        for(int idxRoom: roomList){
            
            listRoom.get(idxRoom).getClients().add(new Model_Client(socket,acc));
        }
    }
    
    
    public void broadcastToRoom(Model_Send_Message message){
        //Client emit ke server, server menerima "broadcast" dan sendEvent/send ke masing" sioc 
        
        //send Event ke masing" client
        //Broadcast ke ruangan yang dituju?
        int roomIdx = message.getId_Room();
        List<Model_Client> clients = listRoom.get(roomIdx).getClients();
        
         for(Model_Client client : clients){
                    
                    if(client.getUser().getUserId()!=message.getFromIdUser() ){
                       
                        client.getClient().sendEvent("broadcast", new Model_Receive_Message(message.getFromIdUser(), message.getId_Room(), message.getText()));
                   }
            }

         
    }

    
}
