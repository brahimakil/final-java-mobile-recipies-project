package com.example.recipieproject.ui.ingredients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.recipieproject.databinding.FragmentIngredientsBinding;
import com.example.recipieproject.model.Recipe;

public class IngredientsFragment extends Fragment {
    private FragmentIngredientsBinding binding;
    private IngredientsViewModel ingredientsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                         ViewGroup container, Bundle savedInstanceState) {
        ingredientsViewModel = new ViewModelProvider(this).get(IngredientsViewModel.class);
        ingredientsViewModel.initDatabase(requireContext());
        binding = FragmentIngredientsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        ingredientsViewModel.generateRandomRecipes();

        binding.fabRefresh.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            ingredientsViewModel.generateRandomRecipes();
        });

        ingredientsViewModel.getRandomRecipes().observe(getViewLifecycleOwner(), recipes -> {
            binding.progressBar.setVisibility(View.GONE);
            if (recipes != null && !recipes.isEmpty()) {
                binding.ingredientsRecyclerView.setAdapter(
                    new IngredientsAdapter(requireContext(), recipes, ingredientsViewModel)
                );
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 