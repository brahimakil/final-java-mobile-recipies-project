package com.example.recipieproject.ui.recipes;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.recipieproject.R;
import com.example.recipieproject.databinding.FragmentRecipesBinding;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.utils.BitmapUtils;
import com.google.android.material.textfield.TextInputEditText;

public class RecipesFragment extends Fragment {
    private FragmentRecipesBinding binding;
    private RecipesViewModel recipesViewModel;
    private RecipeAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                         ViewGroup container, Bundle savedInstanceState) {
        recipesViewModel = new ViewModelProvider(this).get(RecipesViewModel.class);
        binding = FragmentRecipesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recipesViewModel.initDatabase(requireContext());

        adapter = new RecipeAdapter(recipesViewModel);
        binding.recipesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recipesRecyclerView.setAdapter(adapter);

        recipesViewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
            adapter.setRecipes(recipes);
            adapter.notifyDataSetChanged();
        });

        binding.fabAddRecipe.setOnClickListener(v -> showAddRecipeDialog());

        SearchView searchView = binding.searchView;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filterRecipes(newText);
                return true;
            }
        });

        return root;
    }

    private void showAddRecipeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_recipe, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.recipe_title_input);
        TextInputEditText ingredientsInput = dialogView.findViewById(R.id.recipe_ingredients_input);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.recipe_description_input);
        Button predictIngredientsButton = dialogView.findViewById(R.id.predict_ingredients_button);
        Button generateButton = dialogView.findViewById(R.id.generate_description_button);
        ImageView previewImage = dialogView.findViewById(R.id.recipe_preview_image);
        ProgressBar imageProgress = dialogView.findViewById(R.id.image_progress);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_recipe)
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        predictIngredientsButton.setOnClickListener(v -> {
            String recipeName = titleInput.getText().toString().trim();
            if (!recipeName.isEmpty()) {
                predictIngredientsButton.setEnabled(false);
                predictIngredientsButton.setText("Predicting ingredients...");
                
                System.out.println("Predicting ingredients for: " + recipeName);
                
                recipesViewModel.predictIngredients(recipeName)
                    .thenAccept(ingredients -> {
                        System.out.println("Received ingredients: " + ingredients); // Debug log
                        requireActivity().runOnUiThread(() -> {
                            if (ingredients != null && !ingredients.isEmpty()) {
                                ingredientsInput.setText(ingredients);
                            } else {
                                ingredientsInput.setError("Failed to predict ingredients");
                            }
                            predictIngredientsButton.setEnabled(true);
                            predictIngredientsButton.setText(R.string.predict_ingredients);
                        });
                    })
                    .exceptionally(throwable -> {
                        System.out.println("Error predicting ingredients: " + throwable.getMessage()); // Debug log
                        requireActivity().runOnUiThread(() -> {
                            ingredientsInput.setError("Error: " + throwable.getMessage());
                            predictIngredientsButton.setEnabled(true);
                            predictIngredientsButton.setText(R.string.predict_ingredients);
                        });
                        return null;
                    });
            } else {
                titleInput.setError("Please enter a recipe name");
            }
        });

        generateButton.setOnClickListener(v -> {
            String ingredients = ingredientsInput.getText().toString();
            if (!ingredients.isEmpty()) {
                generateButton.setEnabled(false);
                generateButton.setText("Generating...");
                imageProgress.setVisibility(View.VISIBLE);
                
                previewImage.setVisibility(View.GONE);
                recipesViewModel.setCurrentBitmap(null);
                
                recipesViewModel.generateDescription(ingredients)
                    .thenAccept(description -> {
                        requireActivity().runOnUiThread(() -> {
                            descriptionInput.setText(description);
                            
                            recipesViewModel.generateImage(ingredients)
                                .thenAccept(bitmap -> {
                                    requireActivity().runOnUiThread(() -> {
                                        if (bitmap != null) {
                                            previewImage.setImageBitmap(bitmap);
                                            previewImage.setVisibility(View.VISIBLE);
                                            recipesViewModel.setCurrentBitmap(bitmap);
                                            Toast.makeText(requireContext(), "Image generated successfully", Toast.LENGTH_SHORT).show();
                                        }
                                        imageProgress.setVisibility(View.GONE);
                                        generateButton.setEnabled(true);
                                        generateButton.setText(R.string.generate_description);
                                    });
                                })
                                .exceptionally(throwable -> {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "Failed to generate image: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                        imageProgress.setVisibility(View.GONE);
                                        generateButton.setEnabled(true);
                                        generateButton.setText(R.string.generate_description);
                                    });
                                    return null;
                                });
                        });
                    });
            } else {
                ingredientsInput.setError("Please enter ingredients");
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = titleInput.getText().toString();
                String ingredients = ingredientsInput.getText().toString();
                String description = descriptionInput.getText().toString();

                if (!title.isEmpty() && !ingredients.isEmpty() && !description.isEmpty()) {
                    if (imageProgress.getVisibility() == View.VISIBLE) {
                        Toast.makeText(requireContext(), "Please wait for image generation to complete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (recipesViewModel.getCurrentBitmap() == null) {
                        Toast.makeText(requireContext(), "Please generate an image first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Recipe recipe = new Recipe(title, ingredients, description, "");
                    recipe.setImageData(BitmapUtils.bitmapToByteArray(recipesViewModel.getCurrentBitmap()));
                    recipesViewModel.addRecipe(recipe);
                    dialog.dismiss();
                } else {
                    if (title.isEmpty()) titleInput.setError("Required");
                    if (ingredients.isEmpty()) ingredientsInput.setError("Required");
                    if (description.isEmpty()) descriptionInput.setError("Required");
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 
