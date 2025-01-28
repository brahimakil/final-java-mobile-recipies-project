package com.example.recipieproject.ui.recipes;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.services.AIService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecipesViewModel extends ViewModel {
    private MutableLiveData<List<Recipe>> recipes;
    private SQLiteDatabase db;
    private AIService aiService;
    private Bitmap currentBitmap;

    public RecipesViewModel() {
        recipes = new MutableLiveData<>(new ArrayList<>());
        aiService = new AIService();
    }

    public void initDatabase(Context context) {
        db = context.openOrCreateDatabase("recipes.db", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS recipes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "ingredients TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "image_path TEXT, " +
                "image_data BLOB, " +
                "timestamp INTEGER NOT NULL)");
    }

    public LiveData<List<Recipe>> getRecipes() {
        loadRecipes();
        return recipes;
    }

    private void loadRecipes() {
        List<Recipe> recipeList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM recipes ORDER BY timestamp DESC", null);
        
        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe(
                    cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("ingredients")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    cursor.getString(cursor.getColumnIndexOrThrow("image_path"))
                );
                recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                
                int imageDataColumnIndex = cursor.getColumnIndex("image_data");
                if (imageDataColumnIndex != -1) {
                    recipe.setImageData(cursor.getBlob(imageDataColumnIndex));
                }
                
                recipe.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                recipeList.add(recipe);
            } while (cursor.moveToNext());
        }
        cursor.close();
        recipes.postValue(recipeList);
    }

    public void addRecipe(Recipe recipe) {
        db.execSQL("INSERT INTO recipes (title, ingredients, description, image_path, image_data, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{recipe.getTitle(), recipe.getIngredients(), recipe.getDescription(), 
                            recipe.getImagePath(), recipe.getImageData(), System.currentTimeMillis()});
        loadRecipes();
    }

    public CompletableFuture<String> generateDescription(String ingredients) {
        return aiService.generateDescription(ingredients);
    }

    public CompletableFuture<Bitmap> generateImage(String description) {
        return aiService.generateImage(description);
    }

    public CompletableFuture<String> predictIngredients(String recipeName) {
        return aiService.predictIngredients(recipeName);
    }

    public void setCurrentBitmap(Bitmap bitmap) {
        this.currentBitmap = bitmap;
    }

    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    public void searchRecipes(String query) {
        List<Recipe> filteredList = new ArrayList<>();
        for (Recipe recipe : recipes.getValue()) {
            if (recipe.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                recipe.getIngredients().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(recipe);
            }
        }
        recipes.postValue(filteredList);
    }

    public void deleteRecipe(Recipe recipe) {
        db.execSQL("DELETE FROM recipes WHERE title = ? AND timestamp = ?",
                new Object[]{recipe.getTitle(), recipe.getTimestamp()});
        loadRecipes();
    }

    public void updateRecipe(Recipe recipe) {
        db.execSQL("UPDATE recipes SET title = ?, ingredients = ?, description = ?, image_data = ? WHERE id = ?",
                new Object[]{recipe.getTitle(), recipe.getIngredients(), recipe.getDescription(), 
                            recipe.getImageData(), recipe.getId()});
        loadRecipes();
    }
} 