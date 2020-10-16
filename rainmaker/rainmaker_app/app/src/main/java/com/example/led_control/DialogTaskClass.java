package com.example.led_control;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class DialogTaskClass extends AppCompatDialogFragment {

    private EditText edit_task;
    private DialogListener listener;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater =getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_task_dialog,null);

        builder.setView(view).setTitle("Task Name").setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(edit_task.getWindowToken(),0);


                String taskName = edit_task.getText().toString();

                if (!TextUtils.isEmpty(taskName)) {


                    listener.applyText(taskName, getArguments().getBoolean("editMode"), getArguments().getInt("taskId"));


                }
                else {

                    Toast.makeText(getContext(), "Please enter task name first.", Toast.LENGTH_SHORT).show();

                }


            }
        });
    edit_task = view.findViewById(R.id.edit_task);
    edit_task.requestFocus();







    if (getArguments() != null && !TextUtils.isEmpty(getArguments().getString("taskName"))){

        edit_task.setText(getArguments().getString("taskName"));
        edit_task.setSelection(edit_task.getText().length());
    }

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);



    return  builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (DialogListener) context;

        }catch (ClassCastException e){
            throw  new ClassCastException(context.toString()+ "must implement DialogListener");

        }

    }

    public  interface  DialogListener{
        void applyText(String taskName, boolean editMode,int taskId);

    }
}
