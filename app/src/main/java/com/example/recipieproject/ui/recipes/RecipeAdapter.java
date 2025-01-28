package com.example.recipieproject.ui.recipes;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.recipieproject.R;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.utils.BitmapUtils;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import android.content.ActivityNotFoundException;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ProgressBar;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipes = new ArrayList<>();
    private List<Recipe> allRecipes = new ArrayList<>();
    private Context context;
    private RecipesViewModel recipesViewModel;

    public RecipeAdapter(RecipesViewModel viewModel) {
        this.recipesViewModel = viewModel;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.titleView.setText(recipe.getTitle());
        holder.descriptionView.setText(recipe.getDescription());
        
        byte[] imageData = recipe.getImageData();
        if (imageData != null) {
            Bitmap bitmap = BitmapUtils.byteArrayToBitmap(imageData);
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
                holder.imageView.setVisibility(View.VISIBLE);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_recipe_placeholder);
                holder.imageView.setVisibility(View.VISIBLE);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.ic_recipe_placeholder);
            holder.imageView.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> showRecipeDialog(recipe));
        holder.imageView.setOnClickListener(v -> showRecipeDialog(recipe));
    }

    private void showRecipeDialog(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_view_recipe, null);

        ImageView imageView = dialogView.findViewById(R.id.recipe_detail_image);
        TextView titleView = dialogView.findViewById(R.id.recipe_detail_title);
        TextView ingredientsView = dialogView.findViewById(R.id.recipe_detail_ingredients);
        TextView descriptionView = dialogView.findViewById(R.id.recipe_detail_description);

        ImageButton editButton = dialogView.findViewById(R.id.btn_edit);
        ImageButton shareButton = dialogView.findViewById(R.id.btn_share);
        ImageButton deleteButton = dialogView.findViewById(R.id.btn_delete);

        titleView.setText(recipe.getTitle());
        ingredientsView.setText(recipe.getIngredients());
        descriptionView.setText(recipe.getDescription());

        if (recipe.getImageData() != null) {
            Bitmap bitmap = BitmapUtils.byteArrayToBitmap(recipe.getImageData());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_recipe_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_recipe_placeholder);
        }
        imageView.setVisibility(View.VISIBLE);

        editButton.setOnClickListener(v -> {
            showEditDialog(recipe);
        });

        shareButton.setOnClickListener(v -> {
            shareToWhatsApp(recipe);
        });

        deleteButton.setOnClickListener(v -> {
            showDeleteConfirmation(recipe);
        });

        builder.setView(dialogView)
               .setPositiveButton("Close", null)
               .show();
    }

    private void shareToWhatsApp(Recipe recipe) {
        String shareText = String.format("Recipe: %s\n\nIngredients:\n%s\n\nInstructions:\n%s",
                recipe.getTitle(), recipe.getIngredients(), recipe.getDescription());
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setPackage("com.whatsapp");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(Recipe recipe) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe?")
            .setPositiveButton("Delete", (dialog, which) -> {
                recipesViewModel.deleteRecipe(recipe);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditDialog(Recipe recipe) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_recipe, null);
        
        TextInputEditText titleInput = dialogView.findViewById(R.id.recipe_title_input);
        TextInputEditText ingredientsInput = dialogView.findViewById(R.id.recipe_ingredients_input);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.recipe_description_input);
        ImageView previewImage = dialogView.findViewById(R.id.recipe_preview_image);
        ProgressBar imageProgress = dialogView.findViewById(R.id.image_progress);

        titleInput.setText(recipe.getTitle());
        ingredientsInput.setText(recipe.getIngredients());
        descriptionInput.setText(recipe.getDescription());

        if (recipe.getImageData() != null) {
            Bitmap bitmap = BitmapUtils.byteArrayToBitmap(recipe.getImageData());
            if (bitmap != null) {
                previewImage.setImageBitmap(bitmap);
                previewImage.setVisibility(View.VISIBLE);
            }
        }

        String originalIngredients = recipe.getIngredients();

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Edit Recipe")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newTitle = titleInput.getText().toString();
                String newIngredients = ingredientsInput.getText().toString();
                String newDescription = descriptionInput.getText().toString();

                if (!newTitle.isEmpty() && !newIngredients.isEmpty() && !newDescription.isEmpty()) {
                    recipe.setTitle(newTitle);
                    recipe.setDescription(newDescription);

                    if (!newIngredients.equals(originalIngredients)) {
                        recipe.setIngredients(newIngredients);
                        imageProgress.setVisibility(View.VISIBLE);
                        
                        Toast.makeText(context, "Regenerating image for new ingredients...", Toast.LENGTH_LONG).show();
                        
                        recipesViewModel.generateImage(newIngredients)
                            .thenAccept(bitmap -> {
                                if (bitmap != null) {
                                    recipe.setImageData(BitmapUtils.bitmapToByteArray(bitmap));
                                    Toast.makeText(context, "Image regenerated successfully", Toast.LENGTH_SHORT).show();
                                }
                                recipesViewModel.updateRecipe(recipe);
                                dialog.dismiss();
                            });
                    } else {
                        recipesViewModel.updateRecipe(recipe);
                        dialog.dismiss();
                    }
                }
            });
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void setRecipes(List<Recipe> recipes) {
        this.allRecipes = new ArrayList<>(recipes);
        this.recipes = new ArrayList<>(recipes);
        notifyDataSetChanged();
    }

    public void filterRecipes(String query) {
        recipes.clear();
        if (query.isEmpty()) {
            recipes.addAll(allRecipes);
        } else {
            String lowercaseQuery = query.toLowerCase();
            for (Recipe recipe : allRecipes) {
                if (recipe.getTitle().toLowerCase().contains(lowercaseQuery) ||
                    recipe.getIngredients().toLowerCase().contains(lowercaseQuery)) {
                    recipes.add(recipe);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView descriptionView;

        RecipeViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.recipe_image);
            titleView = view.findViewById(R.id.recipe_title);
            descriptionView = view.findViewById(R.id.recipe_description);
        }
    }
} 