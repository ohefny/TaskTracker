package com.example.be.tasktracker;

import android.content.Context;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.ExpandedMenuView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.be.tasktracker.DataModel.HandleData;
import com.example.be.tasktracker.DataModel.Project;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Date;


public class CreateProjectFragment extends Fragment {
    // TODO: Rename parameter arguments, choose  names that match

    private Button addBtn, saveBtn;
    private EditText titleTxt, subtaskTxt;
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> listItems;
    private final int EDIT_ITEM_ID = 2;
    private final int DELETE_ITEM_ID = 1;
    private int selectedItem;
    private JSONObject jsonObject;
    private boolean fileExist;
    private Project project;
    private File file;
    private ArrayList<String> existingProjects=new ArrayList<>();
    private boolean saved = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View rootView = inflater.inflate(R.layout.fragment_create_project, container, false);
        addBtn = (Button) rootView.findViewById(R.id.Addbtn);
        saveBtn = (Button) rootView.findViewById(R.id.saveBtn);
        titleTxt = (EditText) rootView.findViewById(R.id.projectTitle);
        subtaskTxt = (EditText) rootView.findViewById(R.id.subtaskTitle);
        listView = (ListView) rootView.findViewById(R.id.listView);
        listItems = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, listItems);
        listView.setAdapter(arrayAdapter);
        registerForContextMenu(listView);
        LayoutInflater inflator = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      //  View v = inflator.inflate(R.layout.edit_text_actionbar, null);
        handleActions();

        return rootView;


    }

    @Override
    public void onStart() {
        super.onStart();
        file = new File(getActivity().getFilesDir(), getString(R.string.projects_file_name));
        jsonObject=new JSONObject();
        String jsonStr=HandleData.getProjectsJsonStr(getActivity(),file);
        if(jsonStr!=null){
            try {
                jsonObject=new JSONObject(jsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        existingProjects = HandleData.readProjectsNames(getActivity(),file,jsonObject);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(menu.NONE, 1, 1, "Delete");
        menu.add(menu.NONE, 2, 2, "Edit");

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        System.out.println("Fuck method in ");
        if (item.getItemId() == DELETE_ITEM_ID) {
            arrayAdapter.remove(arrayAdapter.getItem(selectedItem));
        } else
            showEditDialog();


        return super.onContextItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            new File(getActivity().getFilesDir(), project.getProjectName()).createNewFile();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleActions() {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = position;
                return false;
            }
        });
        if (titleTxt.getText().toString().length() == 0)
            saveBtn.setEnabled(false);


        titleTxt.addTextChangedListener(getTextChangeListener());
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (subtaskTxt.getText().toString().length() == 0) {
                    Toast.makeText(getActivity(), "Enter Subtask Title", Toast.LENGTH_SHORT).show();

                } else {
                    arrayAdapter.add(subtaskTxt.getText().toString());
                    subtaskTxt.setText("");
                }

            }
        });
        saveBtn.setOnClickListener(getOnSaveListener());
    }

    private TextWatcher getTextChangeListener() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                if (titleTxt.getText().toString().length() == 0)
                    saveBtn.setEnabled(false);
                else
                    saveBtn.setEnabled(true);

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (titleTxt.getText().toString().length() == 0)
                    saveBtn.setEnabled(false);
                else
                    saveBtn.setEnabled(true);

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (titleTxt.getText().toString().length() == 0)
                    saveBtn.setEnabled(false);
                else
                    saveBtn.setEnabled(true);
            }
        };
    }

    private View.OnClickListener getOnSaveListener() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                for (int i = 0; i < existingProjects.size(); i++) {
                    System.out.println(existingProjects.get(i));
                    if (existingProjects.get(i).equalsIgnoreCase(titleTxt.getText().toString())) {
                        showErrorDialog("There exist project with the same title ... try again with another title");
                        return;
                    }
                }
                if (listItems.size() == 0)
                    showErrorDialog("Please add at least one subtask to save the project");
                else {
                    project = new Project(titleTxt.getText().toString(), listItems, System.currentTimeMillis());
                    saved= HandleData.saveNewProject(getActivity(),project,file,jsonObject,saved);
                    if(saved)
                     Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();



                }

            }
        };

    }

    public void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Item");
        final EditText editText = new EditText(getActivity());
        editText.setText(listItems.get(selectedItem));
        builder.setView(editText);

        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listItems.remove(listItems.get(selectedItem));
                listItems.add(selectedItem, editText.getText().toString());
                System.out.println("fuck " + listItems.get(selectedItem));
                arrayAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    public void showErrorDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Error");
        builder.setIcon(R.drawable.error128);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.show();
    }

}





