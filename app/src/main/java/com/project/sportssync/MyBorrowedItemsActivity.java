package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyBorrowedItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerBorrowed;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    
    private String userId;
    private FirebaseFirestore db;
    private List<BorrowRequest> borrowedList;
    private BorrowedItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_borrowed_items);

        userId = getIntent().getStringExtra("userId");
        
        recyclerBorrowed = findViewById(R.id.recyclerBorrowed);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        db = FirebaseFirestore.getInstance();
        borrowedList = new ArrayList<>();

        recyclerBorrowed.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BorrowedItemsAdapter(borrowedList, this::returnItem);
        recyclerBorrowed.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadBorrowedItems);

        loadBorrowedItems();
    }

    private void loadBorrowedItems() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        db.collection("borrowRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    borrowedList.clear();
                    
                    if (querySnapshot.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerBorrowed.setVisibility(View.GONE);
                    } else {
                        recyclerBorrowed.setVisibility(View.VISIBLE);
                        querySnapshot.forEach(doc -> {
                            BorrowRequest request = doc.toObject(BorrowRequest.class);
                            request.setId(doc.getId());
                            borrowedList.add(request);
                        });
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
                });
    }

    private void returnItem(BorrowRequest request) {
        // Update status to "return_pending" - waiting for admin approval
        db.collection("borrowRequests").document(request.getId())
                .update("status", "return_pending")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Return request sent to admin", Toast.LENGTH_SHORT).show();
                    loadBorrowedItems();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
