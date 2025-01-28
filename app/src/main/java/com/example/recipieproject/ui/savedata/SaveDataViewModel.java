package com.example.recipieproject.ui.savedata;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.recipieproject.model.Recipe;
import java.util.ArrayList;
import java.util.List;

public class SaveDataViewModel extends ViewModel {
    private SQLiteDatabase db;
    private MutableLiveData<List<Recipe>> savedRecipes;

    public SaveDataViewModel() {
        savedRecipes = new MutableLiveData<>(new ArrayList<>());
    }

    public void initDatabase(Context context) {
        db = context.openOrCreateDatabase("recipes.db", Context.MODE_PRIVATE, null);
        loadSavedRecipes();
    }

    public LiveData<List<Recipe>> getSavedRecipes() {
        return savedRecipes;
    }

    private void loadSavedRecipes() {
        List<Recipe> recipeList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM recipes ORDER BY timestamp DESC", null);
        
        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe(
                    cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("ingredients")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    ""
                );
                recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                
                int imageDataColumnIndex = cursor.getColumnIndex("image_data");
                if (imageDataColumnIndex != -1) {
                    recipe.setImageData(cursor.getBlob(imageDataColumnIndex));
                }
                
                recipeList.add(recipe);
            } while (cursor.moveToNext());
        }
        cursor.close();
        savedRecipes.postValue(recipeList);
    }
} 