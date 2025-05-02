package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class OrdonnanceAdapter extends ArrayAdapter<Ordonnance> {

    private final Context context;
    private final List<Ordonnance> ordonnanceList;

    public OrdonnanceAdapter(Context context, List<Ordonnance> ordonnanceList) {
        super(context, R.layout.item_ordonnance, ordonnanceList);
        this.context = context;
        this.ordonnanceList = ordonnanceList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_ordonnance, parent, false);

            holder = new ViewHolder();
            holder.imageViewOrdonnance = convertView.findViewById(R.id.image_view_ordonnance);
            holder.textViewNom = convertView.findViewById(R.id.text_view_nom);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Ordonnance ordonnance = ordonnanceList.get(position);
        holder.textViewNom.setText(ordonnance.getNom());
        holder.imageViewOrdonnance.setImageBitmap(decodeBase64(ordonnance.getImageBase64()));

        return convertView;
    }

    private Bitmap decodeBase64(String base64) {
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    static class ViewHolder {
        ImageView imageViewOrdonnance;
        TextView textViewNom;
    }
}