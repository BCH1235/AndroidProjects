package com.am.mytodolistapp.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProjectDetailPagerAdapter extends FragmentStateAdapter {

    private final String projectId;

    public ProjectDetailPagerAdapter(@NonNull Fragment fragment, String projectId) {
        super(fragment);
        this.projectId = projectId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return ProjectTodoListFragment.newInstance(projectId);
            case 1:
                return ProjectMemberListFragment.newInstance(projectId);
            case 2:
                return ProjectActivityFragment.newInstance(projectId);
            default:
                return ProjectTodoListFragment.newInstance(projectId);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}