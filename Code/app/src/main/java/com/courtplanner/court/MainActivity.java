package com.courtplanner.court;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    enum DisplayPage{
        MAIN,
        RESULT
    }

    private PopupWindow popupWindow;

    class Player{
        boolean selected = false;
        int numberSelected = 0;
        String name = "";

        public Player(){
        }

        public Player(String name){
            this.name = name;
        }

        public Player copy(){
            Player p = new Player();
            p.numberSelected = this.numberSelected;
            p.name = this.name;
            p.selected = this.selected;
            return p;
        }
    }


    SharedPreferences sharedPreferences;
    ArrayList<String> drawnPlayers = new ArrayList<String>();

    ArrayList<Player> availablePlayers = new ArrayList<Player>();
    Object mutex = new Object();

    int currentFieldIndex = 1;
    int numberCourts = 4;

    public String getDatabaseString(){
        String res  = "";
        synchronized (mutex){
            for(Player p : availablePlayers){
                res += p.name;
                res += "_";
                res += Integer.toString(p.numberSelected);
                res += "_";
                res += Boolean.toString(p.selected);
                res += "|";
            }

        }
        return res;
    }

    public void parseDatabaseFromString(String database){
        ArrayList<Player> playerList = new ArrayList<>();

        // Split the input string by '|'
        String[] playerDataArray = database.split("\\|");

        for (String playerData : playerDataArray) {
            // Split each player data by '_'
            String[] playerFields = playerData.split("_");

            // Ensure that there are enough fields to create a Player object
            if (playerFields.length == 3) {
                Player player = new Player();

                // Extract and set the player's name
                player.name = playerFields[0];

                // Extract and set the numberSelected as an integer
                try {
                    player.numberSelected = Integer.parseInt(playerFields[1]);
                } catch (NumberFormatException e) {
                    // Handle parsing error as needed
                    player.numberSelected = 0; // Default value
                }

                // Extract and set the selected status as a boolean
                player.selected = Boolean.parseBoolean(playerFields[2]);

                // Add the Player object to the list
                playerList.add(player);
            } else {
                // Handle invalid data format as needed
            }
        }

        availablePlayers = playerList;
    }

    public void increaseNumberSelectedOnPlayer(String name){
        synchronized (mutex){
            Player p = getPlayer(name);
            if(p != null){
                p.numberSelected += 1;
            }
        }
    }

    public ArrayList<Player> copyAvailablePlayers(){
        synchronized (mutex) {
            ArrayList<Player> newAvailablePlayers = new ArrayList<Player>();
            for(Player p : availablePlayers){
                newAvailablePlayers.add(p.copy());
            }
            return newAvailablePlayers;
        }
    }

    public ArrayList<Player> getSelectedPlayers(){
        synchronized (mutex) {
            ArrayList<Player> newAvailablePlayers = new ArrayList<Player>();
            for(Player p : availablePlayers){
                if(p.selected == true) {
                    newAvailablePlayers.add(p.copy());
                }
            }
            return newAvailablePlayers;
        }
    }

    public boolean isPlayerPresent(String name){
        for (Player p : availablePlayers) {
            if (p.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addPlayer(Player p){
        synchronized (mutex) {
            if (!isPlayerPresent(p.name)) {
                availablePlayers.add(p);
            }
        }
    }

    public void removePlayer(String name){
        synchronized (mutex) {
            Iterator<Player> iterator = availablePlayers.iterator();
            while (iterator.hasNext()) {
                Player p2 = iterator.next();
                if (p2.name.equals(name)) {
                    iterator.remove(); // Safely remove the player
                }
            }
        }
    }

    public Player getPlayer(String name){
        synchronized (mutex) {
            if (isPlayerPresent(name)) {
                for (Player p : availablePlayers) {
                    if (p.name.equals(name)) {
                        return p;
                    }
                }
            }
            return null;
        }
    }

    public void selectPlayer(String name){
        Player p = getPlayer(name);
        if(p != null){
            p.selected = true;
        }
    }

    public void deselectPlayer(String name){
        Player p = getPlayer(name);
        if(p != null){
            p.selected = false;
        }
    }

    private DisplayPage currentPage;

    public void setPage(DisplayPage page){
        if(page == DisplayPage.RESULT){
            setContentView(R.layout.activity_result);
            currentPage = page;
        }else if(page == DisplayPage.MAIN){
            setContentView(R.layout.activity_main);
            currentPage = page;
        }
    }

    public DisplayPage getPage(){
        return currentPage;
    }

    public int getNumberSelected(){
        synchronized (mutex) {
            int res = 0;
            for(Player p : availablePlayers){
                if(p.selected == true){
                    res += 1;
                }
            }
            return res;
        }
    }

    public void sortAvailablePlayers(){
        synchronized (mutex) {
            ArrayList<Player> newAvailablePlayers = new ArrayList<Player>();

            while (availablePlayers.size() > 0) {
                int currentMax = 0;
                Player curBest = null;
                for (Player p : availablePlayers) {
                    if (curBest == null) {
                        curBest = p;
                        currentMax = p.numberSelected;
                    } else if (p.numberSelected > currentMax) {
                        curBest = p;
                        currentMax = p.numberSelected;
                    } else if (curBest != null && p.numberSelected == currentMax) {
                        if (p.name.toLowerCase().charAt(0) < curBest.name.toLowerCase().charAt(0)) {
                            curBest = p;
                            currentMax = p.numberSelected;
                        }
                    }
                }
                newAvailablePlayers.add(curBest.copy());
                availablePlayers.remove(curBest);
            }
            availablePlayers.clear();
            availablePlayers = newAvailablePlayers;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("court-planner-cache-marko", MODE_PRIVATE);

        String defaultValue = "---";

        String database = sharedPreferences.getString("Database", defaultValue);

        if(!database.equals(defaultValue)){
            parseDatabaseFromString(database);
        }

        setPage(DisplayPage.MAIN);
        addTextWatcher();
        final ScrollView scrollView = findViewById(R.id.scrollViewPlayers); // Replace with your ScrollView's ID

        // Add a ViewTreeObserver.OnGlobalLayoutListener
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Now, update your ScrollView and other UI components
                refreshScrollView();
                updateSelectedPlayerTextCount();
            }
        });
    }


    public void addTextWatcher(){
        TextInputEditText tv = findViewById(R.id.textInputEditText);
        if(tv == null){
            return;
        }
        // Add a TextWatcher to the TextInputEditText
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called before text is changed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // This method is called when text is changed
                // Call your "update" function here
                refreshScrollView();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // This method is called after text is changed
            }
        });

    }

    public void onButtonClickAddPlayer(View view) {
        TextInputEditText tv = findViewById(R.id.textInputEditText);
        if(tv == null){
            return;
        }
        String newPlayer = tv.getText().toString();
        if (newPlayer.equals("")){
            return;
        }

        addPlayer(new Player(newPlayer.toUpperCase()));
        updateSelectedPlayerTextCount();
        refreshScrollView();
        tv.setText("");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Database", getDatabaseString());
        editor.apply(); // or editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Database", getDatabaseString());
        editor.apply(); // or editor.commit();
        Log.println(Log.ERROR,"DATABASE",getDatabaseString());
    }

    public void onButtonClickResultBack(View view){
        setPage(DisplayPage.MAIN);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                refreshScrollView();
                updateSelectedPlayerTextCount();
            }
        });
    }

    public void onButtonClickResultLeft(View view) {
        currentFieldIndex -= 1;
        if(currentFieldIndex == 0){
            currentFieldIndex = numberCourts;
        }
        updateResultField();

    }

    public void onButtonClickResultRight(View view) {
        currentFieldIndex += 1;
        if(currentFieldIndex == numberCourts + 1){
            currentFieldIndex = 1;
        }
        updateResultField();
    }

    public void onButtonClickDraw(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // Create a PopupWindow
        popupWindow = new PopupWindow(popupView,
                //(int)(width*0.6),  // Set your desired width
                //(int)(height*0.8), // Set your desired height
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set background color for the pop-up window
        // popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        // Show the pop-up in the middle of the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    public void onButtonClickDrawConfirm(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);
        TextInputEditText t = popupView.findViewById(R.id.textInputEditCourts);
        if(t == null){
            return;
        }
        numberCourts = 4;
        try {
            numberCourts = Integer.parseInt(t.getText().toString());
        }catch (Exception e){

        }

        ArrayList<Player> selectedPlayer = getSelectedPlayers();

        drawnPlayers.clear();
        for(int cur_court = 1; cur_court <= numberCourts; ++cur_court) {
            for (int var_field = 0; var_field < 4; ++var_field) {
                if (selectedPlayer.size() == 0) {
                    break;
                }
                Random random = new Random();
                int randomIndex = random.nextInt(selectedPlayer.size());
                Player player = selectedPlayer.get(randomIndex);

                drawnPlayers.add(player.name);
                increaseNumberSelectedOnPlayer(player.name);

                selectedPlayer.remove(player);
            }
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Database", getDatabaseString());
        editor.apply(); // or editor.commit();

        // Close the PopupWindow
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        setPage(DisplayPage.RESULT);
        updateResultField();
    }

    public static String addLineBreaksOnSpaces(String input) {
        return input.replaceAll(" ", "\n");
    }

    public void updateResultField(){
        int startIndex = numberCourts * (currentFieldIndex-1);
        for(int i = 0; i < 4; ++i){
            int textViewId = getResources().getIdentifier("textViewPlayer" + Integer.toString(i+ 1), "id", getPackageName());
            TextView t = findViewById(textViewId);
            if(t == null){
                continue;
            }
            if(drawnPlayers.size() > startIndex + i){

                String displayName = drawnPlayers.get(startIndex + i);
                String modifiedString = addLineBreaksOnSpaces(displayName);

                t.setText(modifiedString);
                // t.setShadowLayer(30f, 30f, 30f, Color.WHITE);
                // t.setShadowLayer(30f, -30f, -30f, Color.BLACK);

            }else{
                t.setText("");
            }
        }

        TextView t = findViewById(R.id.textViewField);
        if(t == null){
            return;
        }
        t.setText("Field: " + Integer.toString(currentFieldIndex) + "/" + Integer.toString(numberCourts));

    }



    public void refreshScrollView(){
        TextInputEditText tv = findViewById(R.id.textInputEditText);
        if(tv == null){
            return;
        }
        String typedName = tv.getText().toString();


        LinearLayout l = findViewById(R.id.scrollViewLinearPlayers);
        if(l == null){
            return;
        }
        l.removeAllViews();

        // DisplayMetrics displayMetrics = new DisplayMetrics();
        // getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // int height = displayMetrics.heightPixels;
        int width = l.getWidth();

        sortAvailablePlayers();
        synchronized (mutex) {
            for (Player p : copyAvailablePlayers()) {
                if (p.name.contains(typedName.toUpperCase())) {
                    LinearLayout.LayoutParams layoutParams_lh = new LinearLayout.LayoutParams((int) (width - 30), dpToPx(40));
                    layoutParams_lh.setMargins(10, 10, 10, 0);
                    LinearLayout lh = new LinearLayout(this);
                    lh.setDividerPadding(100);

                    lh.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams((int) (width * 0.8), dpToPx(40));
                    layoutParams1.setMargins(2, 2, 2, 2);

                    Button b0 = new Button(this);
                    // b0.setWidth(dpToPx(40));
                    // b0.setHeight(dpToPx(40));
                    b0.setText(p.name);
                    b0.setTextSize(18);
                    if (p.selected == true) {
                        b0.setBackgroundColor(getResources().getColor(R.color.colorSelected));
                    } else {
                        b0.setBackgroundColor(getResources().getColor(R.color.buttonColor));
                    }
                    b0.setTextColor(getResources().getColor(R.color.buttonFontColor));
                    b0.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (getPlayer(p.name).selected == true) {
                                deselectPlayer(p.name);
                                b0.setBackgroundColor(getResources().getColor(R.color.buttonColor));
                            } else {
                                selectPlayer(p.name);
                                b0.setBackgroundColor(getResources().getColor(R.color.colorSelected));
                            }
                            updateSelectedPlayerTextCount();
                        }
                    });

                    LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams((int) (width * 0.2), dpToPx(40));
                    layoutParams2.setMargins(2, 2, 2, 2);
                    Button b1 = new Button(this);
                    //b1.setWidth((int)(width * 0.2));
                    //b1.setHeight(dpToPx(40));
                    b1.setText("X");
                    b1.setTextSize(18);
                    b1.setBackgroundColor(getResources().getColor(R.color.buttonColor));
                    b1.setTextColor(getResources().getColor(R.color.buttonFontColor));
                    b1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final String playerNameToRemove = p.name;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    removePlayer(playerNameToRemove);
                                }
                            });
                            refreshScrollView();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("Database", getDatabaseString());
                            editor.apply(); // or editor.commit();
                        }
                    });

                    lh.addView(b1, layoutParams2);
                    lh.addView(b0, layoutParams1);

                    l.addView(lh, layoutParams_lh);

                }
            }

        }
    }

    public void updateSelectedPlayerTextCount(){
        TextView t = findViewById(R.id.textViewSelectedPlayer);
        if(t == null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                t.setText("Selected Players: " + Integer.toString(getNumberSelected()) + "/" + Integer.toString(availablePlayers.size()));
            }
        });
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }


}