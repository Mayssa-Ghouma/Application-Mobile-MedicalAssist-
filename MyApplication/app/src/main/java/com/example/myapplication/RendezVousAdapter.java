package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class RendezVousAdapter extends ArrayAdapter<RendezVous> {

    public RendezVousAdapter(Context context, List<RendezVous> rendezVousList) {
        super(context, 0, rendezVousList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_rendezvous, parent, false);
        }

        RendezVous rdv = getItem(position);

        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvDocteur = convertView.findViewById(R.id.tvDocteur);
        TextView tvHeure = convertView.findViewById(R.id.tvHeure);

        tvDate.setText(rdv.getDate());
        tvDocteur.setText(rdv.getNombreteur());
        tvHeure.setText(rdv.getTime());

        return convertView;
    }
}