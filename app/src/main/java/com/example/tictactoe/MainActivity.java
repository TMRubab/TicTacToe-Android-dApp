package com.example.tictactoe;

import androidx.annotation.MainThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.EventLog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {

    private final static String PRIVATE_KEY = "69a900966a396bdc76ab230b0b88993cad3472a8ecb6571f9b823a8dba2afcb7";

    private final static BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);
    private final static BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);

    private final static String CONTRACT_ADDRESS = "0x6925c37718A2978959F88b9d3c279a7bE33e1b39";
    TicTacToe tictactoe;
    private boolean signOrLog; //true for sign, false for log
    boolean loggedIn = false;
    private int gamePlayed = -1;
    private int gameWin = -1;
    private int ID = -1;
    private String name;
    private int gameSize = 3; //for now
    private String opponentName = "";
    private int turn;//0 for me; 1 for other
    private String currentPlayer[] = new String[2];
    private int gameID = -1;
    private String mySign = "";
    private String theirSign = "";
    enum state{NONE, CREATED, RUNNING, FINISHED}
    state gameState = state.NONE;
    private boolean joined = false; // whether joined or created (false means  either in no game or created a game)
    private int board[][] = new int[3][];
    private boolean sent[][] = new boolean[3][];
    private static int cell = -1; // the most recent move
    enum result{WIN, DRAW, LOSE}
    result gameResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Web3j web3j = Web3j.build(new HttpService("http://10.0.2.2:8545")); //for android
       // Web3j web3j = Web3j.build(new HttpService(/* your wifi IP address */)); //for wifi
        Credentials credentials = getCredentialsFromPrivateKey();
        //deployCon(web3j, credentials);
        tictactoe = loadContract(CONTRACT_ADDRESS, web3j, credentials);
        //first layout
        final Button bttnSignUp = findViewById(R.id.bttnSignUp);
        final Button bttnLogIn = findViewById(R.id.bttnLogIn);
        //sign up or login layout
        final EditText txtID = findViewById(R.id.txtID);
        final EditText txtName = findViewById(R.id.txtName);
        final EditText txtPassword = findViewById(R.id.txtPassword);
        //also for logIn
        final Button bttnSignLogOk = findViewById(R.id.bttnSignUpOk);
        final Button bttnSignLogBack = findViewById(R.id.bttnLogInBack);
        //player's account layout
        final TextView txtViewID = findViewById(R.id.txtViewID);
        final TextView txtViewGamePlayed = findViewById(R.id.txtViewGamePlayed);
        final TextView txtViewGameWin = findViewById(R.id.txtViewGameWin);
        final Button bttnCreateGame = findViewById(R.id.bttnCreateGame);
        final Button bttnJoinGame = findViewById(R.id.bttnJoinGame);
        final Button bttnLogout = findViewById(R.id.bttnLogout);
        //create game layout
        final Spinner txtGameSize = findViewById(R.id.txtGameSize);
        final EditText txtGamePassword = findViewById(R.id.txtGamePassword);
        final Button bttnCreateGameOk = findViewById(R.id.bttnCreateGameOk);
        final Button bttnCreateGameBack = findViewById(R.id.bttnCreateGameBack);
        //join game layout
        final EditText txtGameID = findViewById(R.id.txtGameID);
        final EditText txtJoinGamePassword = findViewById(R.id.txtJoinGamePassword);
        final Button bttnJoinGameOk = findViewById(R.id.bttnJoinGameOk);
        final Button bttnJoinGameBack = findViewById(R.id.bttnJoinGameBack);
        //Tic-Tac-Toe layout
        final TextView txtPlayersName = findViewById(R.id.txtPlayersName);
        final TextView txtCurrentPlayer = findViewById(R.id.txtCurrentPlayer);
        final Button bttnGameStart = findViewById(R.id.bttnGameStart);
        final Button bttnGameBack = findViewById(R.id.bttnGameBack);
        final Button bttnTTT[] = new Button[9];
        bttnTTT[0] = findViewById(R.id.bttn0);
        bttnTTT[1] = findViewById(R.id.bttn1);
        bttnTTT[2] = findViewById(R.id.bttn2);
        bttnTTT[3] = findViewById(R.id.bttn3);
        bttnTTT[4] = findViewById(R.id.bttn4);
        bttnTTT[5] = findViewById(R.id.bttn5);
        bttnTTT[6] = findViewById(R.id.bttn6);
        bttnTTT[7] = findViewById(R.id.bttn7);
        bttnTTT[8] = findViewById(R.id.bttn8);
        for(int i = 0; i < 9; ++i){
            final int iCopy = i;
            bttnTTT[i].setOnClickListener(new View.OnClickListener() {
                public void  onClick(View v){
                    //int j = Integer.parseInt(v.getTag().toString());
                    int j = iCopy;
                    if(turn == 1 || board[j/3][j%3] !=0 || gameState != state.RUNNING) return;
                    //Logical changes
                    cell = j;
                    board[j/3][j%3] = 1;
                    sent[j/3][j%3] = false;
                    turn = 1 - turn;
                    //GUI changes
                    bttnTTT[j].setText(mySign);
                    bttnGameStart.setText("Send");
                    bttnGameStart.setVisibility(View.VISIBLE);
                    txtCurrentPlayer.setText(currentPlayer[turn]);
                    //txtPlayersName.setText(String.valueOf(cell));//for testing
                }
            });
        }

        for(int i = 0; i < 3; ++i){
            board[i] = new int[3];
            sent[i] = new boolean[3];
            for(int j = 0; j < 3; ++j){
                board[i][j] = 0;//0 for none, 1 for me, 2 for opponent
                sent[i][j] = false;
            }
        }
        currentPlayer[0] = "Your Turn";

        bttnSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                findViewById(R.id.loutSignLog).setVisibility(View.INVISIBLE);
                txtName.setHint("Enter your name");
                txtPassword.setHint("Enter Password");
                txtID.setHint("Enter ID");
                findViewById(R.id.loutSignLogInput).setVisibility(View.VISIBLE);
                txtID.setText("Id not required for sign up");
                txtID.setEnabled(false);
                signOrLog = true;
            }
        });
        bttnLogIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                findViewById(R.id.loutSignLog).setVisibility(View.INVISIBLE);
                txtName.setHint("Enter your name");
                txtPassword.setHint("Enter Password");
                txtID.setHint("Enter ID");
                findViewById(R.id.loutSignLogInput).setVisibility(View.VISIBLE);
                txtID.setText("");
                txtID.setEnabled(true);
                signOrLog = false;
            }
        });

        bttnSignLogOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //name
                if(txtName.getText().toString().equals("")){
                    txtName.setHint("Name Required!");
                    return;
                }
                name = txtName.getText().toString();
                //password
                if(txtPassword.getText().toString().equals("")){
                    txtPassword.setHint("Password Required!");
                    return;
                }
                if(signOrLog){
                    //solidity -- sign up
                    Thread thread = signUp(txtName, txtPassword);
                    while(thread.isAlive()){
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    if(txtID.getText().toString().equals("")){
                        txtID.setHint("ID Required!");
                        return;
                    }
                    ID = Integer.valueOf(txtID.getText().toString());
                    loggedIn = false;
                    //solidity -- login
                    Thread thread = login(txtName, txtID, txtPassword);
                    while(thread.isAlive()){
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(!loggedIn) return;
                }
                findViewById(R.id.loutSignLogInput).setVisibility(View.INVISIBLE);
                txtViewID.setText("Player ID: " + ID);
                txtViewGamePlayed.setText("Game Played: " + gamePlayed);
                txtViewGameWin.setText("Game Win: " + gameWin);
                findViewById(R.id.loutGame).setVisibility(View.VISIBLE);
                if(!signOrLog) txtID.setText("");
                txtName.setText("");
                txtPassword.setText("");
            }
        });

        bttnSignLogBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ID = -1;
                name = "";
                findViewById(R.id.loutSignLogInput).setVisibility(View.INVISIBLE);
                findViewById(R.id.loutSignLog).setVisibility(View.VISIBLE);
                if(!signOrLog) txtID.setText("");
                txtName.setText("");
                txtPassword.setText("");
            }
        });

        bttnCreateGame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findViewById(R.id.loutGame).setVisibility(View.INVISIBLE);
                txtGamePassword.setHint("Enter Game Password");
                txtGameSize.setSelection(0);
                findViewById(R.id.loutCreateGame).setVisibility(View.VISIBLE);
            }
        });

        bttnJoinGame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findViewById(R.id.loutGame).setVisibility(View.INVISIBLE);
                txtGameID.setHint("Enter Game ID");
                txtGamePassword.setHint("Enter Game Password");
                findViewById(R.id.loutJoinGame).setVisibility(View.VISIBLE);
            }
        });

        bttnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ID = -1;
                name = "";
                gameWin = -1;
                gamePlayed = -1;
                findViewById(R.id.loutGame).setVisibility(View.INVISIBLE);
                findViewById(R.id.loutSignLog).setVisibility(View.VISIBLE);
            }
        });

        bttnCreateGameOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gameID = -1;
                //password
                if(txtGamePassword.getText().toString().equals("")){
                    txtGamePassword.setHint("Password Required!");
                    return;
                }
                txtGameSize.setSelection(0);
                //from solidity gather data
                Thread thread = createGame(Integer.parseInt((String)txtGameSize.getSelectedItem()), txtGamePassword.getText().toString());
                while(thread.isAlive()){
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(gameID == -1) return;
                //Logical changes
                turn = 0;
                gameState = state.CREATED;
                for(int i = 0; i < 3; ++i){
                    for(int j = 0; j < 3; ++j){
                        board[i][j] = 0;//0 for none, 1 for me, 2 for opponent
                        sent[i][j] = false;
                    }
                }
                cell = -1;
                joined = false;
                //GUI changes
                findViewById(R.id.loutCreateGame).setVisibility(View.INVISIBLE);
                txtPlayersName.setText("Game ID: " + gameID);
                txtCurrentPlayer.setText(currentPlayer[turn]);
                for(int i = 0; i < 9; ++i)
                    bttnTTT[i].setText("");
                bttnGameStart.setText("Start");
                findViewById(R.id.loutTicTacToe).setVisibility(View.VISIBLE);
                txtGamePassword.setText("");
                mySign = "O";
                theirSign = "X";
            }
        });

        bttnCreateGameBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findViewById(R.id.loutCreateGame).setVisibility(View.INVISIBLE);
                findViewById(R.id.loutGame).setVisibility(View.VISIBLE);
                txtGamePassword.setText("");
            }
        });

        bttnJoinGameOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //ID
                if(txtGameID.getText().toString().equals("")){
                    txtGameID.setHint("ID Required!");
                    return;
                }
                //password
                if(txtJoinGamePassword.getText().toString().equals("")){
                    txtJoinGamePassword.setHint("Password Required!");
                    return;
                }
                gameID = Integer.valueOf(txtGameID.getText().toString());
                //from solidity gather data
                Thread thread = joinGame(txtGameID, txtJoinGamePassword);
                while(thread.isAlive()){
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(!joined) return;
                //Logical changes
                turn = 1;
                gameState = state.CREATED;
                for(int i = 0; i < 3; ++i){
                    for(int j = 0; j < 3; ++j){
                        board[i][j] = 0;//0 for none, 1 for me, 2 for opponent
                        sent[i][j] = false;
                    }
                }
                cell = -1;
                opponentName = "";
                //event subscribe -- for finding creator's name
                Thread thread2 = listenJoinGame(true);
                //while(opponentName == ""){
                    try {
                        thread2.join();
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                //}
                if(opponentName == "") return;
                //GUI changes
                findViewById(R.id.loutJoinGame).setVisibility(View.INVISIBLE);
                txtPlayersName.setText(name + " VS " + opponentName);
                currentPlayer[1] = opponentName + "'s Turn";
                txtCurrentPlayer.setText(currentPlayer[turn]);
                for(int i = 0; i < 9; ++i)
                    bttnTTT[i].setText("");
                bttnGameStart.setText("Start");
                txtCurrentPlayer.setText(currentPlayer[turn]);
                findViewById(R.id.loutTicTacToe).setVisibility(View.VISIBLE);
                txtJoinGamePassword.setText("");
                txtGameID.setText("");
                mySign = "X";
                theirSign = "O";
            }
        });

        bttnJoinGameBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gameID = -1;
                findViewById(R.id.loutJoinGame).setVisibility(View.INVISIBLE);
                findViewById(R.id.loutGame).setVisibility(View.VISIBLE);
                txtJoinGamePassword.setText("");
                txtGameID.setText("");
            }
        });

        bttnGameStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                if(turn == 0 && gameState != state.RUNNING && gameState != state.FINISHED) {// if created
                    //event subscribe -- wait for opponent
                    opponentName = "";
                    Thread thread = listenJoinGame(false);
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                    if(opponentName == ""){
                        txtPlayersName.setText("No player found! Please try again.");
                        return;
                    }
                    currentPlayer[1] = opponentName + "'s Turn";
                    txtPlayersName.setText(name + " VS " + opponentName);
                    txtCurrentPlayer.setText(currentPlayer[turn]);
                    gameState = state.RUNNING;
                }
                else if (turn == 1 && gameState != state.RUNNING && gameState != state.FINISHED){//if joined
                    gameState = state.RUNNING;
                    cell = -1;
                    //listen for 1st move event
                    Thread thread = listenNextMove();
                   // while(cell == -1){
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            while(cell == -1){
                                try{
                                    Thread.sleep(6000);
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                }
                            }
                        }
                   // }
                    bttnTTT[cell].setText(theirSign);
                    board[cell/3][cell%3] = 2;
                    turn = 1 - turn;
                    txtCurrentPlayer.setText(currentPlayer[turn]);
                    //gameState = state.RUNNING;
                }
                else if(turn == 1 && gameState == state.RUNNING){ //between gameplay -- for getting move //shown as "Send"
                    //call solidity move function
                    //txtPlayersName.setText(String.valueOf(cell));//for testing
                    if(sent[cell/3][cell%3] || (board[cell/3][cell%3] != 1) || (bttnTTT[cell].getText().toString() != mySign)){
                        for(int i = 0; i < 3; ++i){
                            for(int j = 0; j < 3; ++j){
                                if((bttnTTT[3*i+j].getText().toString() == mySign) && (sent[i][j] == false)){
                                    cell = 3*i + j;
                                    board[i][j] = 1;
                                }
                            }
                        }
                    }
                    //String oldMessage = txtCurrentPlayer.getText().toString();
                    String[] errorMessage = new String[1];
                    errorMessage[0] = "";
                    Thread thread = move(errorMessage);
                    while(thread.isAlive()){
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(errorMessage[0] != ""){
                        //txtCurrentPlayer.setText(newMessage[0] + "... YOU SELECTED: " + cell);
                        board[cell/3][cell%3] = 0;
                        bttnTTT[cell].setText("");
                        txtCurrentPlayer.setText("Send Failed! Please Select Again.");
                        bttnGameStart.setVisibility(View.INVISIBLE);
                        turn = 1-turn;
                        return;
                    }
                    sent[cell/3][cell%3] = true;
                    //check if game ended -- if so, call finishGame, and change gameState
                    if(gameEnded()) {
                        if(gameResult != result.DRAW) {
                            int winner = (gameResult == result.WIN) ? ((joined) ? 2 : 1) : ((joined) ? 1 : 2);
                            Thread thread2 = finishGame(winner);
                            while(thread2.isAlive()) {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(gameResult == result.WIN) txtPlayersName.setText("You Win!");
                            else txtPlayersName.setText("You Lost!");
                        }
                        else{
                            Thread thread2 = finishGame(0); //draw
                            while(thread2.isAlive()) {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            txtPlayersName.setText("Draw");
                        }
                        gameState = state.FINISHED;
                        return;
                    }

                    //listen for other turn -- listen event, check if game ended, change gameState or turn
                    int prevCell = cell;
                    Thread thread3 = listenNextMove();
                    try {
                        thread3.join();
                    } catch (InterruptedException e) {
                        while(cell == prevCell){
                            try {
                                Thread.sleep(6000);
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                        }
                    }
                    board[cell / 3][cell % 3] = 2;
                    bttnTTT[cell].setText(theirSign);
                    if(gameEnded()) {
                        if(gameResult != result.DRAW) {
                            int winner = (gameResult == result.WIN) ? ((joined) ? 2 : 1) : ((joined) ? 1 : 2);
                            Thread thread4 = finishGame(winner);
                            while(thread4.isAlive()) {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(gameResult == result.WIN) txtPlayersName.setText("You Win!");
                            else txtPlayersName.setText("You Lost!");
                        }
                        else{
                            Thread thread4 = finishGame(0); //draw
                            while(thread4.isAlive()) {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            txtPlayersName.setText("Draw");
                        }
                        gameState = state.FINISHED;
                        return;
                    }
                    turn = 1 - turn;
                    txtCurrentPlayer.setText(currentPlayer[turn]);
                }
                bttnGameStart.setVisibility(View.INVISIBLE);
            }
        });

        bttnGameBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //solidity to finish game, if necessary
                if(gameState != state.FINISHED) {
                    int winner = (joined) ? 1 : 2;
                    Thread thread = finishGame(winner);
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //logical changes
                gameID = -1;
                opponentName = "";
                joined = false;
                gameState = state.NONE;
                //GUI changes
                bttnGameStart.setVisibility(View.VISIBLE);
                findViewById(R.id.loutTicTacToe).setVisibility(View.INVISIBLE);
                findViewById(R.id.loutGame).setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean gameEnded(){
        int row = cell/3;
        int col = cell%3;
        boolean gameWonOrLost = (board[row][0] == board[row][1] && board[row][1] == board[row][2]) || (board[0][col] == board[1][col] && board[1][col] == board[2][col]) || (row == col && board[0][0] == board[1][1] && board[1][1] == board[2][2]) || (row + col == 2 && board[0][2] == board[1][1] && board[1][1] == board[2][0]);
        if(gameWonOrLost) gameResult = (board[row][col] == 1) ? result.WIN : result.LOSE;
        boolean gameDrawn = !gameWonOrLost;
        if(gameDrawn) {
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    gameDrawn = gameDrawn && (board[i][j] > 0);
                }
            }
        }
        if(gameDrawn) gameResult = result.DRAW;
        boolean gameFinished = gameWonOrLost || gameDrawn;
        return gameFinished;
    }

    /***Functions for connecting to Back-end***/

    private Thread signUp(TextView txtName, TextView txtPassword){
        Thread thread = new Thread(new Runnable() { // any transaction need to be done in a separate thread
            Boolean b;
            //boolean done;
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
                        gamePlayed = tictactoe.getNumPlayed().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        gameWin = tictactoe.getNumWin().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        ID = tictactoe.getPlayerId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread login(TextView txtName, TextView txtID, TextView txtPassword){
        Thread thread = new Thread(new Runnable() {
            Boolean b;
            //boolean done;
            @Override
            public void run() {
                try  {
                    b = true;
                    tictactoe.login(txtName.getText().toString(), BigInteger.valueOf(Integer.parseInt(txtID.getText().toString())), txtPassword.getText().toString()).send();
                } catch (Exception e) {
                    b = false;
                    if(e.getMessage().indexOf("invalid id!") == -1){
                        txtName.setText("");
                        txtName.setHint("Invalid name or password");
                        txtPassword.setText("");
                        txtPassword.setHint("Invalid name or password");
                    }
                    else{
                        txtID.setText("");
                        txtID.setHint("Invalid ID");
                    }
                }
                if(b) {
                    try {
                        gamePlayed = tictactoe.getNumPlayed().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        gameWin = tictactoe.getNumWin().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*try {
                        ID = tictactoe.getPlayerId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/ // ID given by user when logging in
                }
                loggedIn = b;
            }
        });
        thread.start();
        return thread;
    }

    private Thread createGame(int size, String password){
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
                        gameID = tictactoe.getGameId().send().intValue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread joinGame(EditText txtID, EditText txtPassword){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    tictactoe.joinGame(BigInteger.valueOf(gameID), txtPassword.getText().toString()).send();
                    joined = true;
                } catch (Exception e) {
                    joined = false;
                    if(e.getMessage().indexOf("No such game :(") != -1){
                        txtID.setText("");
                        txtID.setHint("Invalid ID");
                    }
                    else if (e.getMessage().indexOf("Incorrect Password!") != -1){
                        txtPassword.setText("");
                        txtPassword.setHint("Incorrect password");
                    }
                    else{
                        txtID.setText("");
                        txtID.setHint("This game is already full :(");
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread move(String[] errorMessage){
        Thread thread = new Thread(new Runnable() {
            int row = cell/3;
            int col = cell%3;
            @Override
            public void run() {
                try  {
                    tictactoe.move(BigInteger.valueOf(gameID), BigInteger.valueOf(row),BigInteger.valueOf(col)).send();
                } catch (Exception e) {
                    errorMessage[0] = e.getMessage();
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread finishGame(int winner){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    tictactoe.finishGame(BigInteger.valueOf(gameID), BigInteger.valueOf(winner)).send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread listenJoinGame(boolean c){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(!c) {

                    for (int i = 0; i < 10 && opponentName == ""; i++) { //use for loop and run around 10 times
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        tictactoe.playerJoinedGameEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST)
                                .subscribe(event -> {
                                    if (event.id.compareTo(BigInteger.valueOf(gameID)) == 0) opponentName = event.name;
                                });
                    }
                }
                else{
                    for (int i = 0; i < 10 && opponentName == ""; i++) {
                        try{
                            Thread.sleep(6000);} catch (InterruptedException e){e.printStackTrace();}
                        tictactoe.newGameEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST)
                                .subscribe(event -> {
                                    if (event.gameIndex.compareTo(BigInteger.valueOf(gameID)) == 0) {opponentName = event.name;}
                                });
                    }

                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread listenNextMove(){
        Thread thread = new Thread(new Runnable() {
            int j = cell;
            @Override
            public void run() {
                while(cell==j) {
                    try{Thread.sleep(6000);} catch (InterruptedException e){e.printStackTrace();}
                    tictactoe.playerMadeMoveEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST) //make efficient
                            .subscribe(event -> {
                                if (event.gameIndex.compareTo(BigInteger.valueOf(gameID)) == 0) {
                                    cell = Integer.parseInt(event.row.toString())*gameSize;
                                    cell = cell + Integer.parseInt(event.col.toString());

                                }
                            });
                }
            }
        });
        thread.start();
        return thread;
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

    private TicTacToe loadContract(String contractAddress, Web3j web3j, Credentials credentials) {
        return TicTacToe.load(contractAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);
    }

    private Credentials getCredentialsFromPrivateKey() {
        return Credentials.create(PRIVATE_KEY);
    }

    private String deployContract(Web3j web3j, Credentials credentials) throws Exception {
        return TicTacToe.deploy(web3j, credentials, GAS_PRICE, GAS_LIMIT)
                .send()
                .getContractAddress();
    }

}
