package com.greenhackers.multiiamgeuploadtest

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi

import com.bumptech.glide.Glide

import java.util.ArrayList



class MyAdapter(private val context: Context, private val arrayList: ArrayList<Uri>) : BaseAdapter() {

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any {
        return arrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (mInflater != null) {
            convertView = mInflater.inflate(R.layout.list_items, null)
        }

        val imageView = convertView?.findViewById<ImageView>(R.id.imageView)
        val imagePath = convertView?.findViewById<TextView>(R.id.imagePath)

        imagePath?.text = FileUtils.getPath(context, arrayList[position])

        Glide.with(context)
            .load(arrayList[position])
            .into(imageView)

        return convertView
    }
}
