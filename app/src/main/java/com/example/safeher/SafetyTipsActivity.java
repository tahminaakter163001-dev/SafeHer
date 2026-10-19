package com.example.safeher;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SafetyTipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_tips);

        RecyclerView recyclerView =
                findViewById(R.id.safetyTipsRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        List<SafetyTip> tips = new ArrayList<>();

        tips.add(new SafetyTip(
                "Stay Aware",
                "Pay attention to your surroundings, especially in unfamiliar places."
        ));

        tips.add(new SafetyTip(
                "Share Your Plans",
                "When appropriate, let someone you trust know your destination and expected arrival time."
        ));

        tips.add(new SafetyTip(
                "Keep Your Phone Ready",
                "Keep your phone charged and important emergency contacts accessible."
        ));

        tips.add(new SafetyTip(
                "Choose Safe Routes",
                "When possible, use familiar, well-lit routes and trusted transport."
        ));

        tips.add(new SafetyTip(
                "Trust Your Instincts",
                "If a situation feels unsafe, move toward a safer place and seek help."
        ));
        tips.add(new SafetyTip(
                "Emergency Contacts",
                "Save important emergency phone numbers on your phone."
        ));

        tips.add(new SafetyTip(
                "Protect Personal Information",
                "Avoid sharing your live location with unknown people."
        ));

        tips.add(new SafetyTip(
                "Stay Connected",
                "Keep a trusted person informed when travelling alone."
        ));

        tips.add(new SafetyTip(
                "Use Trusted Transport",
                "Choose reliable transport and check your route before leaving."
        ));

        tips.add(new SafetyTip(
                "Ask for Help",
                "Move to a safe public place and contact someone you trust if needed."
        ));
        recyclerView.setAdapter(new SafetyTipAdapter(tips));
    }
}
