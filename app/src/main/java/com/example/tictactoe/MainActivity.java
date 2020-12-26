package com.example.tictactoe;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.EventLog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;
import org.web3j.abi.datatypes.Event;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;


public class MainActivity extends AppCompatActivity {

    private final static String PRIVATE_KEY = "bd3010407a06329f1eeb24ce06eb3af6df796bbb2e89ccfe37fcce7b0579b005";

    private final static BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);
    private final static BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);

    private final static String RECIPIENT = "0x466B6E82CD017923298Db45C5a3Db7c66Cd753C8";

    private final static String CONTRACT_ADDRESS = "0x7f79dA0B9900c5B4aef13D1dFE23E1658B8Cc12E";
    TicTacToe tictactoe;
    private static int loginOrSign = 0;
    private static int gameCreateOrSave = 0;
    private static int numPlayed = -1;
    private static int numWin = -1;
    private int id = -1;
    private int gameId = -1;
    private static String msg="";
    enum state{NONE, CREATED, STARTED, FINISHED}
    state gameState;
//    private int numGames = -1;
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Web3j web3j = Web3j.build(new HttpService("http://10.0.2.2:8545"));
        Credentials credentials = getCredentialsFromPrivateKey();
        //deployCon(web3j, credentials);
        tictactoe = loadContract(CONTRACT_ADDRESS, web3j, credentials);

        final Button bttnSignUp = findViewById(R.id.bttnSignUp);
        final Button bttnLogin= findViewById(R.id.bttnLogin);
        final Button bttnSend= findViewById(R.id.bttnSend);
        final TextView txtName= findViewById(R.id.txtName);
        final TextView txtPassword= findViewById(R.id.txtPassword);
        final Button bttnCreateGame= findViewById(R.id.bttnCreateGame);
        final Button bttnJoinGame= findViewById(R.id.bttnJoinGame);
        bttnCreateGame.setVisibility(View.INVISIBLE);
        bttnJoinGame.setVisibility(View.INVISIBLE);
        final TextView txtViewId= findViewById(R.id.textViewId);
        final TextView txtID= findViewById(R.id.txtID);
        final TextView txtViewGame= findViewById(R.id.txtViewGame);
        final LinearLayout lLayoutStart = findViewById(R.id.lLayoutStart);
        final LinearLayout lLayoutGame = findViewById(R.id.lLayoutGame);
        final Button bttn00 = findViewById(R.id.bttn0);

        bttn00.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                txtViewGame.setText(msg+" game ID: "+String.valueOf(gameId));
            }
        });

         txtID.setVisibility(View.INVISIBLE);
         txtName.setVisibility(View.INVISIBLE);
         txtPassword.setVisibility(View.INVISIBLE);
         bttnSend.setVisibility(View.INVISIBLE);
         txtViewId.setVisibility(View.INVISIBLE);
         lLayoutGame.setVisibility(View.INVISIBLE);


         //tictactoe.getPlayerJoinedGameEvents()
         bttnSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loginOrSign = 1;
                txtID.setVisibility(View.INVISIBLE);
                txtViewId.setVisibility(View.INVISIBLE);
                txtName.setText("");
                txtName.setVisibility(View.VISIBLE);
                txtPassword.setText("");
                txtPassword.setVisibility(View.VISIBLE);
                bttnSend.setVisibility(View.VISIBLE);
                id = -1; numPlayed = -1; numWin = -1;
            }
        });
         bttnLogin.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 loginOrSign = 2;
                 txtViewId.setVisibility(View.INVISIBLE);
                 txtID.setText("");
                 txtID.setVisibility(View.VISIBLE);
                 txtName.setText("");
                 txtName.setVisibility(View.VISIBLE);
                 txtPassword.setText("");
                 txtPassword.setVisibility(View.VISIBLE);
                 bttnSend.setVisibility(View.VISIBLE);
                 txtID.setText("");
                 txtID.setHint("User ID");
                 txtID.setVisibility(View.VISIBLE);
                 txtPassword.setText("");
                 txtPassword.setHint("Password");
                 id = -1; numPlayed = -1; numWin = -1;
             }
         });
         bttnSend.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 if(loginOrSign == 1) {
                     signUp(txtName, txtPassword);
                 }
                 else if(loginOrSign == 2){
                     login(txtName, txtID, txtPassword);
                  }
                 while(id == -1 || numWin == -1 || numPlayed == -1) {
                 }
                 //bttnCreateGame.setText("Join Game");
                 txtID.setVisibility(View.INVISIBLE);
                 txtName.setVisibility(View.INVISIBLE);
                 txtPassword.setVisibility(View.INVISIBLE);
                 bttnSend.setVisibility(View.INVISIBLE);
                 txtViewId.setText("Your ID: " + String.valueOf(id) + " Total Win: " + String.valueOf(numWin) + "/" + String.valueOf(numPlayed));
                 txtViewId.setVisibility(View.VISIBLE);
                 bttnCreateGame.setVisibility(View.VISIBLE);
                 bttnJoinGame.setVisibility(View.VISIBLE);
                 //gameCreateOrSave = 2;
                 }
         });
         bttnCreateGame.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 if(gameCreateOrSave == 0) {
                     txtName.setVisibility(View.VISIBLE);
                     txtName.setText("");
                     txtName.setHint("Game Length");
                     txtPassword.setVisibility(View.VISIBLE);
                     txtPassword.setText("");
                     txtPassword.setHint("Game Password");
                     bttnCreateGame.setText("Save Game");
                     gameCreateOrSave = 1;
                 }
                 else if (gameCreateOrSave == 1){
                     createGame(Integer.parseInt(txtName.getText().toString()), txtPassword.getText().toString());
                     txtName.setVisibility(View.INVISIBLE);
                     /*txtID.setVisibility(View.VISIBLE);
                     txtID.setText("");
                     txtID.setHint("Game ID");
                     bttnCreateGame.setText("Join Game");
                     gameCreateOrSave = 2;*/
                     while(gameId == -1) {
                     }
                     lLayoutStart.setVisibility(View.INVISIBLE);
                     lLayoutGame.setVisibility(View.VISIBLE);
                     txtViewGame.setText(txtViewGame.getText() + String.valueOf(gameId));
                     listenJoinGame(gameId);
                     while(msg == ""){}
                     txtViewGame.setText(msg+" game ID: "+String.valueOf(gameId));
                 }
                 else {
                     joinGame(Integer.parseInt(txtName.getText().toString()), txtPassword.getText().toString());
                     lLayoutStart.setVisibility(View.INVISIBLE);
                     lLayoutGame.setVisibility(View.VISIBLE);
                     gameId = Integer.parseInt(txtName.getText().toString());
                     txtViewGame.setText(txtViewGame.getText() + String.valueOf(gameId));
                 }
             }
         });
         bttnJoinGame.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 txtName.setVisibility(View.VISIBLE);
                 txtName.setText("");
                 txtName.setHint("Game ID");
                 txtPassword.setVisibility(View.VISIBLE);
                 txtPassword.setText("");
                 txtPassword.setHint("Game Password");
                 bttnCreateGame.setText("Save Game");
                 bttnJoinGame.setVisibility(View.INVISIBLE);
                 gameCreateOrSave = 2;
             }
         });
    }

    private void deployCon(Web3j web3j, Credentials credentials){
       Thread thread = new Thread(new Runnable() {
           @Override
           public void run() {
               try  {
                   deployContract(web3j, credentials);//Your code goes here
               } catch (Exception e) {
                    e.printStackTrace();
               }
           }
       });

       thread.start();
   }
    private void signUp(TextView txtName, TextView txtPassword){
        Thread thread = new Thread(new Runnable() {
            Boolean b;
            @Override
            public void run() {
                try  {
                    b = true;
                    tictactoe.addPlayer(txtName.getText().toString(), txtPassword.getText().toString()).send();
                } catch (Exception e) {
                    b = false;
                    e.printStackTrace();
                }
                if(b) {
                    try {
                        numPlayed = tictactoe.getNumPlayed().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        numWin = tictactoe.getNumWin().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        id = tictactoe.getPlayerId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
    private void login(TextView txtName, TextView txtID, TextView txtPassword){
        Thread thread = new Thread(new Runnable() {
            Boolean b;
            @Override
            public void run() {
                try  {
                    b = true;
                    tictactoe.login(txtName.getText().toString(), BigInteger.valueOf(Integer.parseInt(txtID.getText().toString())), txtPassword.getText().toString()).send();
                 } catch (Exception e) {
                    b = false;
                    e.printStackTrace();
                }
                if(b) {
                    try {
                        numPlayed = tictactoe.getNumPlayed().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        numWin = tictactoe.getNumWin().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        id = tictactoe.getPlayerId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void createGame(int size, String password){
        Thread thread = new Thread(new Runnable() {
            Boolean b;
            @Override
            public void run() {
                try  {
                    b = true;
                    tictactoe.createGame(BigInteger.valueOf(size), password).send();
                } catch (Exception e) {
                    b = false;
                    e.printStackTrace();
                }
                if(b) {
                    try {
                        gameId = tictactoe.getGameId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void joinGame(int size, String password){
        Thread thread = new Thread(new Runnable() {
             @Override
            public void run() {
                try  {
                    tictactoe.joinGame(BigInteger.valueOf(size), password).send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private TicTacToe loadContract(String contractAddress, Web3j web3j, Credentials credentials) {
       return TicTacToe.load(contractAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);
   }

    private Credentials getCredentialsFromPrivateKey() {
        return Credentials.create(PRIVATE_KEY);
     }
    private void listenJoinGame(int gameId){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                   // TicTacToe.PlayerJoinedGameEventResponse e;// = new TicTacToe.PlayerJoinedGameEventResponse;
                    //e.id = BigInteger.valueOf(gameId);
                    //return e.name;
                    //Flowable<TicTacToe.PlayerJoinedGameEventResponse> source = tictactoe.playerJoinedGameEventFlowable(new EthFilter());
                    //tictactoe.playerJoinedGameEventFlowable()
                    //do {
                    //}while(tictactoe.getPlayerJoinedGameEvents(new TransactionReceipt()).get(0).id != BigInteger.valueOf(gameId));
                    Flowable<TicTacToe.PlayerJoinedGameEventResponse> source = tictactoe.playerJoinedGameEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);
                    Disposable d=source.subscribe();
                    msg += d.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }
    private String deployContract(Web3j web3j, Credentials credentials) throws Exception {
        return TicTacToe.deploy(web3j, credentials, GAS_PRICE, GAS_LIMIT)
                .send()
                .getContractAddress();
    }

}