/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdaserver;

import static com.sun.javafx.css.SizeUnits.PC;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import static pdaserver.Visor.con;

/**
 *
 * @author Tiago Ventura
 */
public class Waiting extends Thread {

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    /*
                                                                          ┌────────────────────┐
                                                                          │             DEFINIÇÃO             │
                                                                          │                DE                 │   
                                                                          │             VARIAVEIS             │
                                                                          └────────────────────┘   
     */
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int countOla=0;

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------      
    public Waiting(Socket socket) {
        this.socket = socket;
        try {
            out = socket.getOutputStream();
            in = socket.getInputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public void sendMessage(String message) {
        try {
            out.write(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public void run() {
        System.out.println("Entrou");
        while (true) {
            try {
                Thread.sleep(10);
                if (!socket.isConnected()) {
                    socket.close();
                }

                byte[] rcvBytes = new byte[1024];

                in.read(rcvBytes, 0, 1024);
                String recebido = new String(rcvBytes, 0, rcvBytes.length);
                
                recebido = recebido.replace("lololol", "");
                System.out.println(recebido.replaceAll("\n", ""));
                if (recebido.equalsIgnoreCase("")) {
                    countOla++;
                    if (countOla>5) {
                        socket.close();
                        break;
                    }
                }else{
                    countOla=0;
                }
                
                if (recebido.equalsIgnoreCase(".")) {
                    socket.close();
                    break;
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //           Obter codigo
                } else if (recebido.toLowerCase().startsWith("cod")) {
                    try {
                        String t = recebido.split("=")[1];
                        Statement st = con.createStatement();

                        String strSelect = "select * from st where codigo=" + t;

                        ResultSet reader = st.executeQuery(strSelect);
                        reader.next();
                        String responde;
                        responde = reader.getString("codigo").trim() + ";" + reader.getString("ref").trim() + ";" + reader.getString("design").trim() + ";" + reader.getString("stock").trim() + ";" + reader.getString("u_lgaveta").trim() + ";" + reader.getString("u_lcaixa").trim() + ";" + reader.getString("u_lpalete").trim() + ";" + reader.getString("u_nstknet").trim() + ";" + reader.getString("u_obsoleto").trim();

                        String strSelect2 = "select Sum(qtt) as encFor from bi where ndos='2' and fechada='false' and ref='" + reader.getString("ref").trim() + "'";
                        ResultSet reader2 = st.executeQuery(strSelect2);
                        reader2.next();

                        responde += ";" + ((reader2.getString("encfor") == null) ? "0" : reader2.getString("encfor"));

                        String strSelect3 = "select Sum(qtt) as resClient from bi where (ndos='29' or ndos ='46' or ndos ='25' or ndos ='41' or ndos ='1') and fechada='false' and ref=(select ref from st where codigo=" + t + ");";
                        ResultSet reader3 = st.executeQuery(strSelect3);
                        reader3.next();

                        responde += ";" + ((reader3.getString("resClient") == null) ? "0" : reader3.getString("resClient"));

                        String strSelect4 = "select * from stobs where ref=(select ref from st where codigo=" + t + ");";
                        ResultSet reader4 = st.executeQuery(strSelect4);
                        reader4.next();

                        responde += ";" + reader4.getString("u_confirma");

                        System.out.println(responde);

                        sendMessage(responde);

                    } catch (SQLException ex) {
                        sendMessage("nulo");
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);

                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //           Update confirmar stock
                } else if (recebido.toLowerCase().startsWith("upconfirma")) {
                    try {
                        Statement st = con.createStatement();
                        String strSelect = "update stobs set u_confirma=" + recebido.split("=")[1].split(";")[1] + "" + "where ref=(select ref from st where codigo=" + recebido.split("=")[1].split(";")[0] + ");";
                        st.executeUpdate(strSelect);

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //           Obter dossier
                } else if (recebido.toLowerCase().startsWith("getdossier")) {
                    try {
                        Statement st = con.createStatement();
                        

                        String strSelect = "select Count(*) as contar from bi where binum1<qtt and ref!='nsportes' and ref!='' and ref is not null and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + "";
                        ResultSet reader = st.executeQuery(strSelect);
                        reader.next();
                        String responde = reader.getString("contar");
                        strSelect = "select Count(*) as contar from bi where ref!='nsportes' and ref!='' and ref is not null and fechada = '1' and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + "";
                        reader = st.executeQuery(strSelect);
                        reader.next();
                        if (responde.equalsIgnoreCase(reader.getString("contar")) && !responde.equalsIgnoreCase("0") && responde!=null && !responde.equalsIgnoreCase("")) {
                            sendMessage("-1");
                            throw new Exception();
                        }
                        strSelect = "select Count(*) as contar from bi where binum1=qtt and ref!='nsportes' and ref!='' and ref is not null and fechada = '0' and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + "";
                        reader = st.executeQuery(strSelect);
                        reader.next();
                        if (responde.equalsIgnoreCase(reader.getString("contar"))&& !responde.equalsIgnoreCase("0") && responde!=null && !responde.equalsIgnoreCase("")) {
                            sendMessage("-3");
                            throw new Exception();
                        }
                        
                        System.out.println("responde");
                        sendMessage(responde);

                        int contar = Integer.valueOf(responde);

                        String strSelect2 = "select * from bi where binum1<qtt and ref!='nsportes' and fechada = '0' and ref!='' and ref is not null and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + " order by ref ";
                        ResultSet reader2 = st.executeQuery(strSelect2);
                        int co = 0;
                        for (int i = 0; i < contar; i++) {
                            in.read();
                            Thread.sleep(100);
                            reader2.next();
                            System.out.println(co);
                            responde = reader2.getString("ref") + ";" + reader2.getString("design") + ";" + reader2.getString("qtt") + ";" + reader2.getString("codigo") + ";" + ((reader2.getString("binum1") == null) ? "0" : reader2.getString("binum1"));
                            sendMessage(responde);

                            co++;
                        }

                        in.read();
                        Thread.sleep(200);

                        strSelect = "select * from bo where fechada = '0' and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + "";
                        ResultSet reader5 = st.executeQuery(strSelect);
                        reader5.next();
                        String responde3;
                        try {
                            responde3 = reader5.getString("nome");
                        } catch (Exception ex) {
                            responde3 = "Ninguem";
                        }
                        sendMessage(responde3);

                        byte[] rcvBytes6 = new byte[1024];
                        in.read(rcvBytes6, 0, 1024);
                        Thread.sleep(200);

                        try {
                            responde3 = reader5.getString("dataobra");
                        } catch (Exception ex) {
                            responde3 = "Nunca";
                        }

                        sendMessage(responde3);

                        System.out.println("Enviado tudo");

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception e) {

                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //                                      Get Localização
                } else if (recebido.toLowerCase().startsWith("getloc")) {
                    try {
                        Statement st = con.createStatement();

                        String strSelect = "select Count(*) as contar from st where u_" + recebido.split("=")[1].split(";")[0] + "=" + recebido.split("=")[1].split(";")[1] + " and u_obsoleto='0'";
                        ResultSet reader = st.executeQuery(strSelect);
                        reader.next();
                        String responde = reader.getString("contar");
                        sendMessage(responde);

                        int contar = Integer.valueOf(responde);

                        String strSelect2 = "select * from st where u_" + recebido.split("=")[1].split(";")[0] + "=" + recebido.split("=")[1].split(";")[1] + " and u_obsoleto='0' order by ref ";
                        ResultSet reader2 = st.executeQuery(strSelect2);
                        int co = 0;
                        for (int i = 0; i < contar; i++) {
                            byte[] rcvBytes2 = new byte[1024];
                            in.read(rcvBytes2, 0, 1024);
                            Thread.sleep(200);
                            reader2.next();
                            System.out.println(co);
                            responde = reader2.getString("ref") + ";" + reader2.getString("codigo") + ";" + reader2.getString("stock") + ";" + reader2.getString("design") + ";" + reader2.getString("u_lgaveta") + ";" + reader2.getString("u_lcaixa") + ";" + reader2.getString("u_lpalete");
                            sendMessage(responde);
                            co++;
                        }

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //                                      Get Nome dossier
                } else if (recebido.toLowerCase().startsWith("getnomedossier")) {
                    try {
                        Statement st = con.createStatement();
                        System.out.println("OI2");

                        String strSelect = "select * from bo where fechada = '0' and obrano=" + recebido.split("=")[1].split(";")[0] + " and ndos=" + recebido.split("=")[1].split(";")[1] + "";
                        ResultSet reader = st.executeQuery(strSelect);
                        reader.next();
                        String responde;
                        try {
                            responde = reader.getString("nome");
                        } catch (Exception ex) {
                            responde = "Ninguem";
                        }
                        sendMessage(responde);

                        in.read();
                        Thread.sleep(200);

                        try {
                            responde = reader.getString("dataobra");
                        } catch (Exception ex) {
                            responde = "Nunca";
                        }

                        sendMessage(responde);

                        System.out.println("OI");

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //                                      Update dossier
                } else if (recebido.toLowerCase().startsWith("updossier")) {
                    try {
                        Statement st = con.createStatement();

                        System.out.println("Here");
                        String strSelect = "update bi set binum1=" + recebido.split("=")[1].split(";")[2] + "" + "where ref=" + recebido.split("=")[1].split(";")[1] + " and ndos =" + recebido.split("=")[1].split(";")[0].split("-")[0] + " and obrano=" + recebido.split("=")[1].split(";")[0].split("-")[1];

                        System.out.println(strSelect);
                        st.executeUpdate(strSelect);

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                    //                              Update stocks
                } else if (recebido.toLowerCase().startsWith("up")) {
                    try {
                        Statement st = con.createStatement();
                        String strSelect = "update st set u_" + recebido.split("=")[1].split(";")[1] + "=" + recebido.split("=")[1].split(";")[2] + "" + "where codigo=" + recebido.split("=")[1].split(";")[0] + "";
                        st.executeUpdate(strSelect);

                    } catch (SQLException ex) {
                        Logger.getLogger(Visor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                }

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Waiting.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            } catch (Exception ex) {
                Logger.getLogger(Waiting.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
