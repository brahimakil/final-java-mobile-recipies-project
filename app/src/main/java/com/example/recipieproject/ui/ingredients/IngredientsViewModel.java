package com.example.recipieproject.ui.ingredients;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.services.AIService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import android.graphics.Bitmap;

public class IngredientsViewModel extends ViewModel {
    private MutableLiveData<List<Recipe>> randomRecipes;
    private AIService aiService;
    private SQLiteDatabase db;
    private static final int RECIPES_TO_GENERATE = 10;

    public IngredientsViewModel() {
        randomRecipes = new MutableLiveData<>(new ArrayList<>());
        aiService = new AIService();
    }

    public void initDatabase(Context context) {
        db = context.openOrCreateDatabase("recipes.db", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS generated_recipes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "ingredients TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "image_data BLOB, " +
                "is_favorite INTEGER DEFAULT 0, " +
                "timestamp INTEGER NOT NULL)");
    }

    public void saveGeneratedRecipe(Recipe recipe) {
        db.execSQL("INSERT INTO generated_recipes (title, ingredients, description, image_data, timestamp) VALUES (?, ?, ?, ?, ?)",
                new Object[]{recipe.getTitle(), recipe.getIngredients(), recipe.getDescription(), 
                            recipe.getImageData(), System.currentTimeMillis()});
    }

    public Recipe getStoredRecipe(String title) {
        Cursor cursor = db.rawQuery("SELECT * FROM generated_recipes WHERE title = ?", new String[]{title});
        if (cursor.moveToFirst()) {
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
            
            cursor.close();
            return recipe;
        }
        cursor.close();
        return null;
    }

    public LiveData<List<Recipe>> getRandomRecipes() {
        return randomRecipes;
    }

    public void generateRandomRecipes() {
        aiService.generateDescription(
            "Generate 10 creative and unique recipe titles. Make them sound professional and appetizing. " +
            "Mix different cuisines and cooking styles. Format: Just the titles, one per line. Dont return anything except what i asked for"
        ).thenAccept(titlesResponse -> {
            List<Recipe> recipes = new ArrayList<>();
            String[] titles = titlesResponse.split("\n");
            for (String title : titles) {
                String cleanTitle = title.replaceAll("^\\d+\\.\\s*", "").trim();
                if (!cleanTitle.isEmpty()) {
                    recipes.add(new Recipe(cleanTitle, "", "", ""));
                }
            }
            randomRecipes.postValue(recipes);
        });
    }

    public CompletableFuture<Recipe> generateRecipeDetails(Recipe recipe) {
        return aiService.predictIngredients(recipe.getTitle())
            .thenCompose(ingredients -> {
                recipe.setIngredients(ingredients);
                return aiService.generateDescription(ingredients)
                    .thenApply(description -> {
                        recipe.setDescription(description);
                        return recipe;
                    });
            });
    }

    public CompletableFuture<Bitmap> generateImage(String ingredients) {
        return aiService.generateImage(ingredients);
    }

    public void saveToMainDatabase(Recipe recipe) {
        db.execSQL("INSERT INTO recipes (title, ingredients, description, image_data, timestamp) VALUES (?, ?, ?, ?, ?)",
                new Object[]{recipe.getTitle(), recipe.getIngredients(), recipe.getDescription(), 
                            recipe.getImageData(), System.currentTimeMillis()});
    }
} 