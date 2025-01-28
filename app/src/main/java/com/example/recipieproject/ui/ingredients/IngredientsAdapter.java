package com.example.recipieproject.ui.ingredients;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.recipieproject.R;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.utils.BitmapUtils;
import java.util.List;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {
    private final List<Recipe> recipes;
    private final Context context;
    private final IngredientsViewModel viewModel;

    public IngredientsAdapter(Context context, List<Recipe> recipes, IngredientsViewModel viewModel) {
        this.context = context;
        this.recipes = recipes;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.titleView.setText(recipe.getTitle());
        
        holder.itemView.setOnClickListener(v -> {
            showRecipeDetailsDialog(recipe);
        });
    }

    private void showRecipeDetailsDialog(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_recipe_details, null);
        
        TextView titleView = dialogView.findViewById(R.id.recipe_title);
        TextView ingredientsView = dialogView.findViewById(R.id.recipe_ingredients);
        TextView descriptionView = dialogView.findViewById(R.id.recipe_description);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        ImageButton saveButton = dialogView.findViewById(R.id.btn_save);
        
        titleView.setText(recipe.getTitle());
        
        Recipe storedRecipe = viewModel.getStoredRecipe(recipe.getTitle());
        if (storedRecipe != null) {
            progressBar.setVisibility(View.GONE);
            ingredientsView.setText(storedRecipe.getIngredients());
            descriptionView.setText(storedRecipe.getDescription());
            saveButton.setVisibility(View.GONE); 
        } else {
            saveButton.setOnClickListener(v -> {
                if (recipe.getIngredients().isEmpty() || progressBar.getVisibility() == View.VISIBLE) {
                    Toast.makeText(context, "Please wait for recipe generation to complete", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                progressBar.setVisibility(View.VISIBLE);
                saveButton.setEnabled(false);
                
                viewModel.generateImage(recipe.getIngredients())
                    .thenAccept(bitmap -> {
                        if (bitmap != null) {
                            recipe.setImageData(BitmapUtils.bitmapToByteArray(bitmap));
                        }
                        viewModel.saveToMainDatabase(recipe);
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Recipe saved successfully", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            saveButton.setVisibility(View.GONE);
                        });
                    });
            });
            
            viewModel.generateRecipeDetails(recipe)
                .thenAccept(updatedRecipe -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressBar.setVisibility(View.GONE);
                        ingredientsView.setText(updatedRecipe.getIngredients());
                        descriptionView.setText(updatedRecipe.getDescription());
                    });
                });
        }
        
        AlertDialog dialog = builder.setView(dialogView)
                                  .setPositiveButton("Close", null)
                                  .create();
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;

        ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.recipe_title);
        }
    }
} 