/*
 This file is part of Privacy Friendly 2048. This app implements the functions of the
 game 2048 in a privacy friendly version.

 Privacy Friendly 2048 is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Example is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendly2048.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.secuso.privacyfriendly2048.R;
import org.secuso.privacyfriendly2048.ceramic.*;
import android.widget.FrameLayout;
import org.secuso.privacyfriendly2048.activities.helper.BaseActivityWithoutNavBar;
import org.secuso.privacyfriendly2048.activities.helper.GameConfirmation;
import org.secuso.privacyfriendly2048.activities.helper.GameState;
import org.secuso.privacyfriendly2048.activities.helper.GameStatistics;
import org.secuso.privacyfriendly2048.activities.helper.Gestures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;

/**
 * This activity contains the entire game and draws the game field depending on the selected mode and the screen size.
 * Also the Intent influences, when a new game is generated or a previous game status is loaded.
 * Therefore a two dimensional field named elements is filled with instances of the class Element depending on the selected mode (4x4, 5x5, 6x6, 7x7).
 * The function of the two buttons restart and undo are also implemented.
 * With the help of the class Gesture the user inputs are recognized and the corresponding method is retrieved.
 * This method contains the important game logic for 2048.
 * With loops the values of the elements will be added and the whole elements will be moved in the selected direction.
 * After each move (only if something has been moved) a new element appears on the game field with the help of the method addNumber().
 * This new element is placed randomly on a free spot of the game field.
 * If the activity is paused or stopped, the game status is saved in the GameState and the GameStatistics will be updated.
 *
 * @author Julian Wadephul and Saskia Jacob
 * @version 20180910
 */

@SuppressWarnings("StringConcatenationInLoop")
public class GameActivity extends BaseActivityWithoutNavBar {
    private int n = 4;
    public TextView textFieldPoints;
    public TextView textFieldRecord;
    public int numberFieldSize = 0;
    private Element[][] elements = null;
    private Element[][] last_elements = null;
    private Element[][] backgroundElements;
    private GameState gameState = null;

    RelativeLayout number_field;
    RelativeLayout number_field_background;
    RelativeLayout touch_field;
    View restartButton;
    View undoButton;
    private CeramicScene scene;
    private CeramicAudio audio;
    private int points = 0;
    private int last_points = 0;
    private long record = 0;

    public static long ADDINGSPEED = 100;
    public static long MOVINGSPEED = 80;
    public static long SCALINGSPEED = 100;
    public static float SCALINGFACTOR = 1.1f;

    private boolean moved = false;
    private boolean newGame;
    public boolean won2048 = false;
    private boolean gameOver = false;
    private boolean undo = false;
    private boolean animationActivated = true;
    private boolean saveState = true;

    public final int WINTHRESHOLD = 2048;
    public final double PROPABILITYFORTWO = 0.9;
    View.OnTouchListener swipeListener;


    private String filename;
    private boolean boardReady;
    private androidx.appcompat.app.AlertDialog restartConfirmation;

    static String saveFileName(int size) {
        return "state" + size + ".txt";
    }

    SharedPreferences sharedPref;

    GameStatistics gameStatistics = new GameStatistics(n);
    private long startingTime;
    public int highestNumber;

    private Activity myActivity;


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_settings);
        if(item!=null)item.setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        startingTime = System.currentTimeMillis();
        if(audio!=null)audio.resume();
    }

    @Override
    public void onPause() {
        if (boardReady) save();
        if(audio!=null)audio.pause();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        myActivity = this;
        int requestedSize = getIntent().getIntExtra("n", 4);
        n = requestedSize >= 4 && requestedSize <= 7 ? requestedSize : 4;
        filename = saveFileName(n);
        boolean fromHome = getPackageName().equals(getCallingPackage());
        boolean hasSave = new File(getFilesDir(), filename).isFile();
        boolean startNew = !hasSave || (state == null && fromHome && getIntent().getBooleanExtra("new", false));

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        animationActivated = sharedPref.getBoolean("pref_animationActivated", true);
        if (sharedPref.getBoolean("switch_preference_1", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        audio=new CeramicAudio(this);
        installCeramic();
        initResources();
        number_field.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right <= left || bottom <= top) return;
                v.removeOnLayoutChangeListener(this);
                // Adding tiles inside RelativeLayout's layout callback can leave their
                // bodies at 0x0 until a later layout. Populate after this traversal so
                // both layers receive a normal measure/layout pass before interaction.
                v.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    sizeBoard();
                    initializeState(startNew);
                    initialize();
                    setListener();
                    boardReady = true;
                    if (startNew) saveStateToFile(gameState);
                });
            }
        });
    }

    private void handleUndo() {
        undoButton.setEnabled(false);
        if (undo && last_elements != null) {
            audio.play(CeramicAudio.Cue.UNDO);
            gameStatistics.undo();
            elements = last_elements;
            points = last_points;
            number_field.removeAllViews();
            //number_field_background.removeAllViews();
            points = last_points;
            textFieldPoints.setText("" + points);
            setDPositions(false);
            for (Element[] i : elements) {
                for (Element j : i) {
                    j.setVisibility(View.INVISIBLE);
                    number_field.addView(j);
                    j.drawItem();
                }
            }
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    elements[i][j].setOnTouchListener(swipeListener);
                    backgroundElements[i][j].setOnTouchListener(swipeListener);
                }
            }
            undo = false;
            updateGameState();
            drawAllElements(elements);
            number_field.refreshDrawableState();
        }
        undo = false;
        refreshUndo();
    }
    private void installCeramic(){
        scene=new CeramicScene(this,"game");
        scene.text("game-subtitle",n+" × "+n+" · "+CeramicAssets.words(this,"目标 2048","Reach 2048"));
        scene.action("game-back",View.NO_ID,getString(R.string.cancel),this::finish);
        scene.action("game-menu",View.NO_ID,CeramicAssets.words(this,"更多","More"),()->CeramicNavigation.menu(this,scene.node("game-menu")));
        FrameLayout host=new FrameLayout(this);host.setClipChildren(false);host.setClipToPadding(false);host.setBackground(CeramicAssets.skin(this,"tray-light"));
        number_field_background=new RelativeLayout(this);number_field_background.setId(R.id.number_field_background);
        number_field=new RelativeLayout(this);number_field.setId(R.id.number_field);
        touch_field=new RelativeLayout(this);touch_field.setId(R.id.touch_field);
        for(RelativeLayout layer:new RelativeLayout[]{number_field_background,number_field,touch_field}){
            layer.setClipChildren(false);layer.setClipToPadding(false);host.addView(layer,new FrameLayout.LayoutParams(-1,-1));
        }
        scene.replace("game-board",host);scene.move("game-board",24,298,384,384);
        scene.node("game-score-value").setId(R.id.points);scene.node("game-best-value").setId(R.id.record);
        scene.action("undo",R.id.undoButton,getString(R.string.undo),()->{});
        scene.action("restart",R.id.restartButton,getString(R.string.restart_game),()->{});
        scene.landscapeGame();CeramicScene.install(this,scene);
    }
    private void refreshUndo(){undoButton.setVisibility(View.VISIBLE);undoButton.setEnabled(undo);undoButton.setAlpha(undo?1f:.48f);}
    public void initResources() {
        number_field = (RelativeLayout) findViewById(R.id.number_field);
        number_field_background = (RelativeLayout) findViewById(R.id.number_field_background);
        touch_field = (RelativeLayout) findViewById(R.id.touch_field);
        textFieldPoints = (TextView) findViewById(R.id.points);
        textFieldRecord = (TextView) findViewById(R.id.record);
        restartButton = findViewById(R.id.restartButton);
        restartButton.setOnClickListener(v -> {
            if (!boardReady || (restartConfirmation != null && restartConfirmation.isShowing())) return;
            audio.play(CeramicAudio.Cue.SELECT);
            restartConfirmation = GameConfirmation.show(this, R.string.confirm_restart_title,
                    getString(R.string.confirm_restart_message), R.string.restart_game,
                    this::createNewGame);
        });
        undoButton = findViewById(R.id.undoButton);

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleUndo();
            }
        });

        //number_field.setBackgroundColor((this.getResources().getColor(R.color.background_gamebord)));
        startingTime = Calendar.getInstance().getTimeInMillis();
    }

    private void save() {
        saveStateToFile(gameState);
        gameStatistics.addTimePlayed(Math.max(0L, System.currentTimeMillis() - startingTime));
        saveStatisticsToFile(gameStatistics);
    }

    @Override
    protected void onDestroy() {
        if (restartConfirmation != null) restartConfirmation.dismiss();
        if(audio!=null)audio.release();
        super.onDestroy();
    }

    private void createNewGame() {
        audio.play(CeramicAudio.Cue.SELECT);
        saveStatisticsToFile(gameStatistics);
        initializeState(true);
        initialize();
        saveStateToFile(gameState);
    }

    private void sizeBoard() {
        android.view.ViewGroup.LayoutParams lp = number_field.getLayoutParams();
        if (number_field.getHeight() > number_field.getWidth())
            lp.height = number_field.getWidth();
        else
            lp.width = number_field.getHeight();
        number_field.setLayoutParams(lp);
        number_field_background.setLayoutParams(lp);
    }

    private void initializeState(boolean startNew) {
        gameOver = false;
        won2048 = false;
        gameState = startNew ? null : readStateFromFile();
        newGame = gameState == null;
        if (newGame) {
            gameState = new GameState(n);
            gameState.n = n;
        }
        points = gameState.points;
        last_points = gameState.last_points;
        undo = gameState.undo;
        elements = new Element[n][n];
        last_elements = new Element[n][n];
        backgroundElements = new Element[n][n];
        saveState = true;
    }

    public void drawAllElements(Element[][] e) {
        for (Element[] i : e) {
            for (Element j : i) {
                j.drawItem();
            }
        }
    }

    public void updateGameState() {
        gameState = new GameState(elements, last_elements);
        gameState.n = n;
        gameState.points = points;
        gameState.last_points = last_points;
        gameState.undo = undo;
        updateHighestNumber();
        check2048();
        refreshUndo();

    }

    public void initialize() {
        Log.i("activity", "initialize");
        gameStatistics = readStatisticsFromFile();
        record = gameStatistics.getRecord();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        numberFieldSize = number_field.getWidth();
        if (numberFieldSize > number_field.getHeight())
            numberFieldSize = number_field.getHeight();
        int abstand=Math.round(numberFieldSize*9f/384), inset=Math.round(numberFieldSize*16f/384);
        int number_size=(numberFieldSize-2*inset-(n-1)*abstand)/n;

        textFieldRecord.setText("" + record);
        textFieldPoints.setText("" + points);
        if (undo)
            undoButton.setVisibility(View.VISIBLE);
        else
            undoButton.setEnabled(false);
        refreshUndo();

        number_field_background.removeAllViews();
        number_field.removeAllViews();
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                //background elements
                backgroundElements[i][j] = new Element(this);
                //backgroundElements[i][j].setVisibility(View.INVISIBLE);

                elements[i][j] = new Element(this);
                elements[i][j].setNumber(gameState.getNumber(i, j));

                elements[i][j].drawItem();
                if (elements[i][j].getNumber() >= WINTHRESHOLD)
                    won2048 = true;
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(number_size, number_size);
                lp.setMarginStart(inset + j * (number_size + abstand));
                lp.topMargin = inset + i * (number_size + abstand);
                elements[i][j].setDPosition(lp.getMarginStart(), lp.topMargin);
                elements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setOnTouchListener(swipeListener);
                elements[i][j].setOnTouchListener(swipeListener);
                number_field_background.addView(backgroundElements[i][j]);
                number_field.addView(elements[i][j]);
            }
        }
        last_elements = deepCopy(elements);
        if (undo) {
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    last_elements[i][j].setNumber(gameState.getLastNumber(i, j));
                }
            }
        }
        if (newGame) {
            moved = true;
            addNumber();
            moved = true;
            addNumber();
            newGame = false;
        }
    }

    public void switchElementPositions(Element e1, Element e2) {
        int i = e1.getdPosX();
        int j = e1.getdPosY();

        e1.animateMoving = true;
        e1.setDPosition(e2.getdPosX(), e2.getdPosY());
        e2.animateMoving = false;
        e2.setDPosition(i, j);

    }

    public Element[][] deepCopy(Element[][] e) {
        Element[][] r = new Element[e.length][];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Element[e[i].length];
            for (int j = 0; j < r[i].length; j++) {
                r[i][j] = e[i][j].copy();
            }
        }
        return r;
    }

    private boolean handleSwipeTop() {
        Element[][] temp = deepCopy(elements);
        int temp_points = points;
        moved = false;
        Element s = new Element(myActivity);

        for (int i = 0; i < elements.length; i++) {
            s.number = elements[0][i].number;
            s.posX = 0;
            s.posY = i;


            for (int j = 1; j < elements[i].length; j++) {
                if (elements[j][i].number != 0 && (s.number == 0 || s.number == elements[j][i].number)) {
                    moved = true;
                    elements[j][i].setNumber(s.number + elements[j][i].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                    Element z = elements[j][i];
                    elements[j][i] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;
                    if (s.number != 0)
                        points += elements[s.posX][s.posY].number;
                    if (s.number != 0)
                        s.posX++;
                    j = s.posX;
                    s.number = elements[j][i].number;

                } else if (elements[j][i].number != 0) {
                    s.number = elements[j][i].number;
                    s.posX = j;
                    s.posY = i;
                }
            }

        }
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[0][i].number;
            s.posX = 0;
            s.posY = i;


            for (int j = 1; j < elements[i].length; j++) {
                if (elements[j][i].number != 0 && s.number == 0) {
                    moved = true;
                    elements[j][i].setNumber(s.number + elements[j][i].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                    Element z = elements[j][i];
                    elements[j][i] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;
                    if (s.number != 0)
                        s.posX++;
                    j = s.posX;
                    s.number = elements[j][i].number;

                } else if (s.number != 0) {
                    s.number = elements[j][i].number;
                    s.posX = j;
                    s.posY = i;
                }
            }

        }
        if (moved) {
            gameStatistics.addMoves(1);
            last_points = temp_points;
            last_elements = temp;
            undoButton.setVisibility(View.VISIBLE);
            undo = true;
        }
        if (moved)
            gameStatistics.moveT();
        addNumber();
        setDPositions(animationActivated);
        updateGameState();
        //es wurde nach oben gewischt, hier den Code einfügen
        return false;
    }

    private boolean handleSwipeRight() {
        Element[][] temp = deepCopy(elements);
        int temp_points = points;
        moved = false;
        Element s = new Element(myActivity);
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[i][elements[i].length - 1].number;
            s.posX = i;
            s.posY = elements[i].length - 1;


            for (int j = elements[i].length - 2; j >= 0; j--) {
                if (elements[i][j].number != 0 && (s.number == 0 || s.number == elements[i][j].number)) {
                    moved = true;

                    elements[i][j].setNumber(s.number + elements[i][j].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                    Element z = elements[i][j];
                    elements[i][j] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;

                    if (s.number != 0)
                        points += elements[s.posX][s.posY].number;
                    if (s.number != 0)
                        s.posY--;
                    j = s.posY;
                    s.number = elements[i][j].number;
                } else if (elements[i][j].number != 0) {
                    s.number = elements[i][j].number;
                    s.posX = i;
                    s.posY = j;
                }
            }

        }
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[i][elements[i].length - 1].number;
            s.posX = i;
            s.posY = elements[i].length - 1;


            for (int j = elements[i].length - 2; j >= 0; j--) {
                if (elements[i][j].number != 0 && s.number == 0) {
                    moved = true;

                    elements[i][j].setNumber(s.number + elements[i][j].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                    Element z = elements[i][j];
                    elements[i][j] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;


                    if (s.number != 0)
                        s.posY--;
                    j = s.posY;
                    s.number = elements[i][j].number;
                } else if (s.number != 0) {
                    s.number = elements[i][j].number;
                    s.posX = i;
                    s.posY = j;
                }
            }

        }
        if (moved) {
            gameStatistics.addMoves(1);
            last_points = temp_points;
            last_elements = temp;
            undoButton.setVisibility(View.VISIBLE);
            undo = true;
        }
        if (moved)
            gameStatistics.moveR();
        addNumber();
        setDPositions(animationActivated);
        updateGameState();

        //es wurde nach rechts gewischt, hier den Code einfügen
        return false;
    }

    private boolean handleSwipeLeft() {
        Element[][] temp = deepCopy(elements);
        int temp_points = points;
        moved = false;
        Element s = new Element(myActivity);
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[i][0].number;
            s.posX = i;
            s.posY = 0;


            for (int j = 1; j < elements[i].length; j++) {
                if (elements[i][j].number != 0 && (s.number == 0 || s.number == elements[i][j].number)) {
                    moved = true;


                    elements[i][j].setNumber(s.number + elements[i][j].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                    Element z = elements[i][j];
                    elements[i][j] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;

                    if (s.number != 0)
                        points += elements[s.posX][s.posY].number;
                    if (s.number != 0)
                        s.posY++;
                    j = s.posY;
                    s.number = elements[i][j].number;
                } else if (elements[i][j].number != 0) {
                    s.number = elements[i][j].number;
                    s.posX = i;
                    s.posY = j;
                }
            }

        }
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[i][0].number;
            s.posX = i;
            s.posY = 0;

            for (int j = 1; j < elements[i].length; j++) {
                if (elements[i][j].number != 0 && s.number == 0) {
                    moved = true;

                    elements[i][j].setNumber(s.number + elements[i][j].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                    Element z = elements[i][j];
                    elements[i][j] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;

                    if (s.number != 0)
                        s.posY++;
                    j = s.posY;
                    s.number = elements[i][j].number;
                } else if (s.number != 0) {
                    s.number = elements[i][j].number;
                    s.posX = i;
                    s.posY = j;
                }
            }

        }
        if (moved) {
            gameStatistics.addMoves(1);
            last_points = temp_points;
            last_elements = temp;
            undoButton.setVisibility(View.VISIBLE);
            undo = true;
        }
        if (moved)
            gameStatistics.moveL();
        addNumber();
        setDPositions(animationActivated);
        updateGameState();
        //es wurde nach links gewischt, hier den Code einfügen
        return false;
    }

    private boolean handleSwipeBottom() {
        Element[][] temp = deepCopy(elements);
        int temp_points = points;
        moved = false;
        Element s = new Element(myActivity);
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[elements[i].length - 1][i].number;
            s.posX = elements[i].length - 1;
            s.posY = i;


            for (int j = elements[i].length - 2; j >= 0; j--) {
                if (elements[j][i].number != 0 && (s.number == 0 || s.number == elements[j][i].number)) {
                    moved = true;

                    elements[j][i].setNumber(s.number + elements[j][i].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                    Element z = elements[j][i];
                    elements[j][i] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;

                    if (s.number != 0)
                        points += elements[s.posX][s.posY].number;
                    if (s.number != 0)
                        s.posX--;
                    j = s.posX;
                    s.number = elements[j][i].number;
                } else if (elements[j][i].number != 0) {
                    s.number = elements[j][i].number;
                    s.posX = j;
                    s.posY = i;
                }
            }

        }
        for (int i = 0; i < elements.length; i++) {
            s.number = elements[elements[i].length - 1][i].number;
            s.posX = elements[i].length - 1;
            s.posY = i;


            for (int j = elements[i].length - 2; j >= 0; j--) {
                if (elements[j][i].number != 0 && s.number == 0) {
                    moved = true;

                    elements[j][i].setNumber(s.number + elements[j][i].number);
                    elements[s.posX][s.posY].setNumber(0);
                    switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                    Element z = elements[j][i];
                    elements[j][i] = elements[s.posX][s.posY];
                    elements[s.posX][s.posY] = z;

                    if (s.number != 0)
                        s.posX--;
                    j = s.posX;
                    s.number = elements[j][i].number;
                } else if (s.number != 0) {
                    s.number = elements[j][i].number;
                    s.posX = j;
                    s.posY = i;
                }
            }

        }
        if (moved) {
            gameStatistics.addMoves(1);
            last_points = temp_points;
            last_elements = temp;
            undoButton.setVisibility(View.VISIBLE);
            undo = true;
        }
        if (moved)
            gameStatistics.moveD();
        addNumber();
        setDPositions(animationActivated);
        updateGameState();
        //es wurde nach unten gewischt, hier den Code einfügen
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!boardReady) return super.onKeyDown(keyCode, event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                handleSwipeTop();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                handleSwipeRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                handleSwipeLeft();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                handleSwipeBottom();
                return true;
            case KeyEvent.KEYCODE_Z:
                if (event.isCtrlPressed()) {
                    handleUndo();
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setListener() {
        swipeListener = new Gestures(this) {
            public boolean onSwipeTop() {
                return handleSwipeTop();
            }

            public boolean onSwipeRight() {
                return handleSwipeRight();
            }

            public boolean onSwipeLeft() {
                return handleSwipeLeft();
            }

            public boolean onSwipeBottom() {
                return handleSwipeBottom();
            }

            public boolean nichts() {
                //es wurde keine wischrichtung erkannt, hier den Code einfügen
                return false;
            }
        };
        View.OnTouchListener delegate=swipeListener;
        swipeListener=(v,e)->{v.getParent().requestDisallowInterceptTouchEvent(e.getActionMasked()!=android.view.MotionEvent.ACTION_UP&&e.getActionMasked()!=android.view.MotionEvent.ACTION_CANCEL);return delegate.onTouch(v,e);};
        touch_field.setClickable(true);
        touch_field.setOnTouchListener(swipeListener);
        number_field.setOnTouchListener(swipeListener);
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                elements[i][j].setOnTouchListener(swipeListener);
                backgroundElements[i][j].setOnTouchListener(swipeListener);
            }
        }
    }

    public String display(Element[][] e) {
        String result = "\n";
        for (int i = 0; i < e.length; i++) {
            for (int j = 0; j < e[i].length; j++)
                result = result + " " + elements[i][j].number; //+ " "+elements[i][j];
            result = result + "\n";
        }
        result += "\n";
        for (int i = 0; i < e.length; i++) {
            for (int j = 0; j < e[i].length; j++)
                result = result + " (" + elements[i][j].getX() + " , " + elements[i][j].getY() + ")" + " v:" + elements[i][j].getVisibility();//+" "+elements[i][j];
            result = result + "\n";
        }
        return result;
    }

    public void updateHighestNumber() {
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                if (highestNumber < elements[i][j].number) {
                    highestNumber = elements[i][j].number;
                    gameStatistics.setHighestNumber(highestNumber);
                }
            }
        }
    }

    public void check2048() {
        if (won2048 == false) {
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    if (elements[i][j].number == WINTHRESHOLD) {

                        saveStatisticsToFile(gameStatistics);
                        //MESSAGE
                        new AlertDialog.Builder(this)
                                .setTitle((this.getResources().getString(R.string.Titel_V_Message)))
                                .setMessage((this.getResources().getString(R.string.Winning_Message)))
                                .setNegativeButton((this.getResources().getString(R.string.No_Message)), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onBackPressed();

                                    }
                                })
                                .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setCancelable(false)
                                .create().show();
                        won2048 = true;
                    }
                }
            }
        }
    }


    public void setDPositions(boolean animation) {
        long SCALINGSPEED = GameActivity.SCALINGSPEED;
        long ADDINGSPEED = GameActivity.ADDINGSPEED;
        long MOVINGSPEED = GameActivity.MOVINGSPEED;
        boolean scale = true;
        if (!animation) {
            SCALINGSPEED = 1;
            ADDINGSPEED = 1;
            MOVINGSPEED = 1;
            scale = false;
        }
        for (Element[] i : elements) {
            for (Element j : i) {
                if (j.dPosX != j.getX()) {
                    if (j.animateMoving && animation) {
                        if (j.number != j.dNumber)
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, scale)).start();
                        else
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    } else {
                        if (!animation) {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.leftMargin = j.dPosX;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        } else
                            j.animate().x(j.dPosX).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    }

                }
                if (j.dPosY != j.getY()) {
                    if (j.animateMoving && animation) {
                        if (j.number != j.dNumber)
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, scale)).start();
                        else
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    } else {
                        if (!animation) {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.topMargin = j.dPosY;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        } else
                            j.animate().y(j.dPosY).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    }


                }
            }
        }
    }

    class MovingListener extends AnimatorListenerAdapter {
        Element e = null;
        long SCALINGSPEED = 100;
        float scalingFactor = 1.5f;
        boolean scale = false;

        public MovingListener(Element e, boolean scale) {
            super();
            this.e = e;
            this.SCALINGSPEED = GameActivity.SCALINGSPEED;
            this.scalingFactor = GameActivity.SCALINGFACTOR;
            this.scale = scale;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            animation.setupEndValues();
            if (e != null)
                e.drawItem();
        }

        @Override
        public void onAnimationPause(Animator animation) {
            super.onAnimationPause(animation);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (e != null) {
                e.drawItem();
                if (scale)
                    e.animate().scaleX(scalingFactor).scaleY(scalingFactor).setDuration(SCALINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new ScalingListener(e)).start();
            }

        }
    }

    class ScalingListener extends AnimatorListenerAdapter {
        Element e = null;

        public ScalingListener(Element e) {
            super();
            this.e = e;
        }

        public ScalingListener() {
            super();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            animation.setupEndValues();
        }

        @Override
        public void onAnimationPause(Animator animation) {
            super.onAnimationPause(animation);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (e != null) {
                e.animate().scaleX(1.0f).scaleY(1.0f).setDuration(SCALINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                    }
                }).start();
            }

        }
    }

    public void addNumber() {

        if (points > record) {
            record = points;
            gameStatistics.setRecord(record);
            textFieldRecord.setText("" + record);
        }
        if (moved) {
            gameOver = false;
            moved = false;
            textFieldPoints.setText("" + points);
            Element[] empty_fields = new Element[n * n];
            int counter = 0;
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    if (elements[i][j].number == 0) {
                        empty_fields[counter++] = elements[i][j];
                    }
                }
            }
            if (counter > 0) {
                int index = (int) (Math.random() * counter);
                int number = 2;
                if (Math.random() > PROPABILITYFORTWO)
                    number = 4;

                empty_fields[index].setNumber(number);
                empty_fields[index].drawItem();
                if (animationActivated) {
                    empty_fields[index].setAlpha(0);
                    empty_fields[index].animate().alpha(1).setInterpolator(new LinearInterpolator()).setStartDelay(MOVINGSPEED).setDuration(ADDINGSPEED).start();
                }
                if (counter == 1) {
                    gameOver = true;
                    for (int i = 0; i < elements.length; i++) {
                        for (int j = 0; j < elements[i].length; j++) {
                            if ((i + 1 < elements.length && elements[i][j].number == elements[i + 1][j].number) || (j + 1 < elements[i].length && elements[i][j].number == elements[i][j + 1].number)) {
                                gameOver = false;
                            }
                        }
                    }
                }
            }
            if(boardReady&&!newGame){
                int largest=0;for(Element[] row:elements)for(Element tile:row)largest=Math.max(largest,tile.number);
                audio.play(gameOver?CeramicAudio.Cue.END:(!won2048&&largest>=WINTHRESHOLD)?CeramicAudio.Cue.CLEAR:points>last_points?CeramicAudio.Cue.MERGE:CeramicAudio.Cue.SLIDE);
            }
            updateGameState();

            if (gameOver) {
                gameOver();
            }
        }
        Log.i("number of elements", "" + number_field.getChildCount() + ", " + number_field_background.getChildCount());
    }

    public void gameOver() {
        Log.i("record", "" + record + ", " + gameStatistics.getRecord());
        saveStatisticsToFile(gameStatistics);
        new AlertDialog.Builder(this)
                .setTitle((this.getResources().getString(R.string.Titel_L_Message, points)))
                .setMessage(this.getResources().getString(R.string.Lost_Message, points))
                .setNegativeButton((this.getResources().getString(R.string.No_Message)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveState = false;
                        deleteStateFile();
                        finish();

                    }
                })
                .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createNewGame();
                    }
                })
                .setCancelable(false)
                .create().show();
        Log.i("record", "danach");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_settings) {
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveStateToFile(GameState state) {
        if (!saveState || state == null) return;
        AtomicFile file = new AtomicFile(new File(getFilesDir(), filename));
        FileOutputStream raw = null;
        try {
            raw = file.startWrite();
            ObjectOutputStream out = new ObjectOutputStream(raw);
            out.writeObject(state);
            out.flush();
            file.finishWrite(raw);
        } catch (IOException e) {
            file.failWrite(raw);
            Log.e("GameActivity", "Could not save game", e);
        }
    }

    private void deleteStateFile() {
        new AtomicFile(new File(getFilesDir(), filename)).delete();
    }

    private GameState readStateFromFile() {
        AtomicFile file = new AtomicFile(new File(getFilesDir(), filename));
        try (ObjectInputStream in = new ObjectInputStream(file.openRead())) {
            Object value = in.readObject();
            if (!(value instanceof GameState)) return null;
            GameState state = (GameState) value;
            if (state.n != n || state.numbers == null || state.numbers.length != n * n) return null;
            if (state.undo && (state.last_numbers == null || state.last_numbers.length != n * n)) {
                state.undo = false;
            }
            for (int tile : state.numbers) {
                if (tile > 0) return state;
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.w("GameActivity", "Could not read saved game", e);
        }
        return null;
    }

    public GameStatistics readStatisticsFromFile() {
        GameStatistics gS = new GameStatistics(n);
        try {
            File file = new File(getFilesDir(), "statistics" + n + ".txt");
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            gS = (GameStatistics) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gS;
    }

    public void saveStatisticsToFile(GameStatistics gS) {
        try {
            File file = new File(getFilesDir(), gS.getFilename());
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(gS);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
