package com.example.exencrypt2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {


    private File xmlFile;
    private final String fileName = "register.xml";
    private String tagData = "data";
    private boolean fileExists = false;

    int contador = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xmlFile = new File(getFileStreamPath(fileName).getPath());

        //declaro el cuadro de texto y con el listener se quedará el campo vacío al hacer click
        final EditText editText_encrypt = findViewById(R.id.editText_encrypt);
        editText_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText_encrypt.setText("");
            }
        });

        //declaro botón para encriptar y un textView que informe OK cuando se ha clickado
        final TextView result = findViewById(R.id.result);
        final Button buttonEncrypt = findViewById(R.id.button_encrypt);
        buttonEncrypt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fileExists = xmlFile.exists();
                final EditText text = findViewById(R.id.editText_encrypt);
                contador++;
                saveText(text.getText().toString(), contador);
                result.setText("OK");
            }
        });
    }



    protected void saveText(String text, int contador) {
        try {
            //RandomAcessFile se utiliza para acceder a un fichero de forma aleatoria.
            //Modo RW abre el fichero en modo lectura y escritura. Si el fichero no existe se crea.

            RandomAccessFile randomAccessFile = null;
            String lastLine = null;
            if (fileExists) {
                try {
                    randomAccessFile = new RandomAccessFile(xmlFile, "rw");
                    randomAccessFile.seek(0);

                    final Scanner scanner = new Scanner(xmlFile);
                    int lastLineOffset = 0;
                    int lastLineLength = 0;

                    while (scanner.hasNextLine()) {
                        // para leer linea por linea y conocer el length del fichero y de la última linea
                        lastLine = scanner.nextLine();
                        lastLineLength = lastLine.length() +2;
                        lastLineOffset += lastLineLength;
                    }

                    // No hace falta coger la ultima linea </content_file> lastLineOffset indica el
                    //lugar donde se pondrá el puntero para escribir
                    lastLineOffset -= lastLineLength;

                    // hemos cogido toda la string hasta la última linea menos el tag </content_file>
                    randomAccessFile.seek(lastLineOffset);

                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    randomAccessFile = new RandomAccessFile(xmlFile, "rw");
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            //Creamos el serializer
            XmlSerializer xmlSerializer = Xml.newSerializer();
            //Para escribir las etiquetas xml
            StringWriter writer = new StringWriter();
            ////Asignamos el resultado del serializer al fichero
            xmlSerializer.setOutput(writer);

            if (!fileExists) {
                xmlSerializer.startDocument("UTF-8", true);
                xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializer.startTag(null, "content_file");
            } else {
                xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            }

            saveData(xmlSerializer, text, contador);

            if (!fileExists) {
                xmlSerializer.endTag(null, "content_file");
            }

            xmlSerializer.flush();

            //finalizar doc
            if (lastLine != null) {
                xmlSerializer.endDocument();

                // Append la última linea que será el cierre del tag content_file
                writer.append("\r\n"+lastLine);
            }

            randomAccessFile.writeBytes(writer.toString());
            randomAccessFile.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected String encryptText(String text) {
        try {
            RSA rsa = new RSA();

            //le asignamos el Contexto
            rsa.setContext(getBaseContext());

            //Generamos un juego de claves
            rsa.genKeyPair(1024);

            //Guardamos en la memoria las claves
            rsa.saveToDiskPrivateKey("rsa.pri");
            rsa.saveToDiskPublicKey("rsa.pub");

            //Ciframos
            String encode_text = null;
            encode_text = rsa.Encrypt(text);

            return encode_text;
        } catch (Exception e) {
            return "";
        }


    }

    // Guarda la etiqueta Data de con el formato correcto usando el xmlSerializer
    protected void saveData(XmlSerializer xmlSerializer, String text, int contador) {

        try {
            xmlSerializer.startTag(null, tagData);
            xmlSerializer.attribute(null,"numero", String.valueOf(contador));
            xmlSerializer.startTag(null, "time");
            xmlSerializer.text(Calendar.getInstance().getTime().toString());
            xmlSerializer.endTag(null, "time");
            xmlSerializer.startTag(null, "text");
            xmlSerializer.text(text);
            xmlSerializer.endTag(null, "text");
            xmlSerializer.startTag(null, "cipher_text");
            xmlSerializer.text(encryptText(text));
            xmlSerializer.endTag(null, "cipher_text");
            xmlSerializer.endTag(null, tagData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}