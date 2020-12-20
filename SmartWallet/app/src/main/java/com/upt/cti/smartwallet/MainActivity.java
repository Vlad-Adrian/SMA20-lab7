package com.upt.cti.smartwallet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.upt.cti.smartwallet.model.MonthlyExpenses;

import java.util.ArrayList;

import static com.google.android.gms.common.util.ArrayUtils.contains;

public class MainActivity extends AppCompatActivity {

    private TextView tStatus;
    private EditText eSearch, eIncome, eExpenses;

    private DatabaseReference databaseReference;
    ValueEventListener databaseListener;
    private final String[] months = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tStatus = findViewById(R.id.tStatus);
        eSearch = findViewById(R.id.eSearch);
        eIncome = findViewById(R.id.eIncome);
        eExpenses = findViewById(R.id.eExpenses);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();


        SharedPreferences sharedPreferences = getSharedPreferences("lastMonth", MODE_PRIVATE);
        eSearch.setText(sharedPreferences.getString("month", ""));

    }

    @Override
    protected void onResume() {
        super.onResume();
        getMonths(m -> {
            Spinner spinner = findViewById(R.id.spinnerMonths);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, m);
            spinner.setAdapter(adapter);
            spinner.setSelection(0, false);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!parent.getItemAtPosition(position).toString().toLowerCase().equals(eSearch.getText().toString()))
                        eSearch.setText(parent.getItemAtPosition(position).toString().toLowerCase());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        });
    }

    private void createNewDBListener(String currentMonth) {
        // remove previous databaseListener
        if (databaseReference != null && currentMonth != null && databaseListener != null)
            databaseReference.child("calendar").child(currentMonth).removeEventListener(databaseListener);

        databaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    MonthlyExpenses monthlyExpense = dataSnapshot.getValue(MonthlyExpenses.class);
                    // explicit mapping of month name from entry key
                    monthlyExpense.month = dataSnapshot.getKey();

                    eIncome.setText(String.valueOf(monthlyExpense.getIncome()));
                    eExpenses.setText(String.valueOf(monthlyExpense.getExpenses()));
                    tStatus.setText("Found entry for " + currentMonth);
                } catch (NullPointerException e) {
                    eIncome.setText(String.valueOf(0));
                    eExpenses.setText(String.valueOf(0));
                    tStatus.setText("There is no entry for " + currentMonth);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        // set new databaseListener
        databaseReference.child("calendar").child(currentMonth).addValueEventListener(databaseListener);
    }

    public void clicked(View view) {
        String currentMonth;
        switch (view.getId()) {
            case R.id.bSearch:
                if (!eSearch.getText().toString().isEmpty()) {
                    // save text to lower case (all our months are stored online in lower case)
                    currentMonth = eSearch.getText().toString().toLowerCase();
                    SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("lastMonth", MODE_PRIVATE).edit();
                    sharedPreferencesEditor.putString("month", currentMonth);
                    sharedPreferencesEditor.apply();

                    tStatus.setText("Searching ...");
                    createNewDBListener(currentMonth);
                } else {
                    Toast.makeText(this, "Search field may not be empty", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bUpdate:
                if (!eIncome.getText().toString().isEmpty() && !eExpenses.getText().toString().isEmpty() &&
                        !eSearch.getText().toString().isEmpty() && contains(months, eSearch.getText().toString().toLowerCase())) {
                    currentMonth = eSearch.getText().toString().toLowerCase();
                    float income = Float.parseFloat(eIncome.getText().toString());
                    float expenses = Float.parseFloat(eExpenses.getText().toString());

                    updateDb(currentMonth, income, expenses);
                } else {
                    Toast.makeText(this, "I don't know what to update! Please complete the search field with a month of the year",
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void updateDb(String month, float income, float expenses) {
        MonthlyExpenses monthlyExpenses = new MonthlyExpenses(month, income, expenses);
        databaseReference.child("calendar").child(month).child("income").setValue(income);
        databaseReference.child("calendar").child(month).child("expenses").setValue(expenses);
    }

    private void getMonths(FirebaseCallback callback) {
        ArrayList<String> firebaseMonths = new ArrayList<String>();
        Query query = databaseReference.child("calendar").orderByValue();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot month : snapshot.getChildren()) {
                        firebaseMonths.add(month.getKey());
                    }
                    callback.onCallBack(firebaseMonths);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Spinner spinner = findViewById(R.id.spinnerMonths);
        String[] items = firebaseMonths.toArray(new String[0]);
        System.out.println(items.toString());

    }

    private interface FirebaseCallback {
        void onCallBack(ArrayList<String> m);
    }
}

