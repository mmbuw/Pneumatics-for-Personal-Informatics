package com.example.led_control;




import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class RecyclerAdapter extends RecyclerView.Adapter <RecyclerAdapter.ViewHolder>{


    List<String> tasksList;

    private Context mContext;

    public RecyclerAdapter(List<String> tasksList) {
        this.tasksList = tasksList;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        LayoutInflater layoutInflater= LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.row_item,parent,false);

        ViewHolder viewHolder = new ViewHolder(view);



        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String taskName=tasksList.get(position);





        holder.textViewTask.setText(tasksList.get(position));
        holder.textViewId.setText(String.valueOf(position+1));
        //holder.editTextTask.requestFocus();

//        if (tasksList.get(position) == "wtf"){
//
//        holder.editTextTask.setVisibility(View.VISIBLE);
//        holder.textViewTask.setVisibility(View.GONE);
//        holder.buttonTask.setVisibility(View.VISIBLE);
//        holder.imageViewTask.setVisibility(View.GONE);
//
//        holder.editTextTask.setText("");
//
//        holder.frameTask.setBackgroundColor(Color.parseColor("#BDBDBD"));
//
//        }







    }

    @Override
    public int getItemCount() {

        return tasksList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


        TextView textViewTask;
        EditText editTextTask;
        FrameLayout frameTask;
        Button buttonTask;
        ImageView imageViewTask;
        TextView textViewId;





        public ViewHolder(@NonNull final View itemView) {
            super(itemView);



            textViewTask = itemView.findViewById(R.id.textViewTask);
            //editTextTask = itemView.findViewById(R.id.edittextTask);
            frameTask = itemView.findViewById(R.id.frameTask);

            textViewId= itemView.findViewById(R.id.textViewId);




            itemView.setOnClickListener(this);


//            itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//
//
//                    editTextTask.setVisibility(View.VISIBLE);
//                    textViewTask.setVisibility(View.GONE);
//                    buttonTask.setVisibility(View.VISIBLE);
//                    imageViewTask.setVisibility(View.GONE);
//
//                    frameTask.setBackgroundColor(Color.parseColor("#1B732A"));
//
//
//
//
//
//                    return true;
//                }
//            });












        }

        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(),String.valueOf(getAdapterPosition()),Toast.LENGTH_SHORT).show();
        }
    }
}
