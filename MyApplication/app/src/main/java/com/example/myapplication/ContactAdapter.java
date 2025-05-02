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

public class ContactAdapter extends ArrayAdapter<Contact> {

    private final Context context;
    private final List<Contact> contactList;

    public ContactAdapter(Context context, List<Contact> contactList) {
        super(context, R.layout.item_contact, contactList);
        this.context = context;
        this.contactList = contactList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_contact, parent, false);

            holder = new ViewHolder();
            holder.imageViewContact = convertView.findViewById(R.id.image_view_contact);
            holder.textViewNom = convertView.findViewById(R.id.text_view_nom_contact);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Contact contact = contactList.get(position);
        holder.textViewNom.setText(contact.getNom());
        holder.imageViewContact.setImageBitmap(decodeBase64(contact.getImageBase64()));

        return convertView;
    }

    private Bitmap decodeBase64(String base64) {
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    static class ViewHolder {
        ImageView imageViewContact;
        TextView textViewNom;
    }
}
