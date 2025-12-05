import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Github, Lock } from "lucide-react";

interface CompareFormData {
    owner: string;
    repo: string;
    baseBranch: string;
    headBranch: string;
}

interface User {
    id: string;
    name: string;
    avatar_url: string;
    email: string;
}

const CompareForm: React.FC = () => {
    const [formData, setFormData] = useState<CompareFormData>({
        owner: '',
        repo: '',
        baseBranch: 'main',
        headBranch: ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [user, setUser] = useState<User | null>(null);
    const [authLoading, setAuthLoading] = useState(true);
    const [compareId, setCompareId] = useState<number | null>(null);
    const [prSuggestion, setPrSuggestion] = useState<{ title: string, description: string } | null>(null);
    const [generatingPR, setGeneratingPR] = useState(false);

    useEffect(() => {
        checkAuthentication();
    }, []);

    const checkAuthentication = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/user', {
                withCredentials: true,
            });
            setUser(response.data);
        } catch (error) {
            setUser(null);
        } finally {
            setAuthLoading(false);
        }
    };

    const handleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/github';
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        // Validation
        if (!formData.owner || !formData.repo || !formData.baseBranch || !formData.headBranch) {
            setError('All fields are required');
            return;
        }

        console.log('=== Compare Form Submission ===');
        console.log('Form Data:', formData);
        console.log('Owner:', formData.owner);
        console.log('Repository:', formData.repo);
        console.log('Base Branch:', formData.baseBranch);
        console.log('Head Branch:', formData.headBranch);
        console.log('===============================');

        setLoading(true);

        try {
            const response = await fetch('http://localhost:8080/api/compare', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            console.log('=== API Response ===');
            console.log('Response Data:', data);
            console.log('===================');

            if (response.ok && data.success) {
                setSuccess('Comparison completed successfully!');
                console.log('Files Changed:', data.data?.filesChanged);
                console.log('Additions:', data.data?.additions);
                console.log('Deletions:', data.data?.deletions);

                // Store the comparison ID for PR generation
                if (data.data?.id) {
                    setCompareId(data.data.id);
                    setPrSuggestion(null); // Reset previous suggestion
                }
            } else {
                setError(data.error || 'Failed to compare branches');
            }
        } catch (err) {
            console.error('Error:', err);
            setError('Failed to connect to server');
        } finally {
            setLoading(false);
        }
    };

    const handleGeneratePR = async () => {
        if (!compareId) {
            setError('No comparison result available');
            return;
        }

        setGeneratingPR(true);
        setError(null);

        try {
            const response = await fetch(`http://localhost:8080/api/compare/${compareId}/generate-pr-suggestion`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setPrSuggestion(data.data);
            } else {
                setError(data.error || 'Failed to generate PR suggestion');
            }
        } catch (err) {
            console.error('Error:', err);
            setError('Failed to connect to server');
        } finally {
            setGeneratingPR(false);
        }
    };

    if (authLoading) {
        return (
            <div className="w-full max-w-2xl mx-auto">
                <Card className="p-8 bg-gray-900/50 backdrop-blur-lg border-purple-500/30 shadow-2xl">
                    <div className="flex items-center justify-center py-12">
                        <div className="animate-pulse text-gray-400">Loading...</div>
                    </div>
                </Card>
            </div>
        );
    }

    return (
        <div className="w-full max-w-2xl mx-auto">
            <Card className="p-8 bg-gray-900/50 backdrop-blur-lg border-purple-500/30 shadow-2xl relative overflow-hidden">
                {/* Login Required Overlay */}
                {!user && (
                    <div className="absolute inset-0 bg-gray-900/95 backdrop-blur-sm z-10 flex items-center justify-center p-8">
                        <div className="text-center space-y-6 max-w-md">
                            <div className="flex justify-center">
                                <div className="p-4 bg-purple-500/20 rounded-full">
                                    <Lock className="w-12 h-12 text-purple-400" />
                                </div>
                            </div>
                            <div className="space-y-2">
                                <h3 className="text-2xl font-bold text-white">
                                    Authentication Required
                                </h3>
                                <p className="text-gray-400">
                                    Please sign in with your GitHub account to access the branch comparison feature.
                                    This ensures secure access to your repositories.
                                </p>
                            </div>
                            <Button
                                onClick={handleLogin}
                                size="lg"
                                className="gap-2 h-12 px-8"
                            >
                                <Github className="w-5 h-5" />
                                Sign in with GitHub
                            </Button>
                        </div>
                    </div>
                )}

                <h2 className="text-3xl font-bold text-center mb-6 pixel-gradient bg-clip-text text-transparent">
                    Compare Branches
                </h2>
                <p className="text-gray-400 text-center mb-8">
                    Compare two branches and view the changes
                </p>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label htmlFor="owner" className="block text-sm font-medium text-gray-300 mb-2">
                            Repository Owner
                        </label>
                        <input
                            type="text"
                            id="owner"
                            name="owner"
                            value={formData.owner}
                            onChange={handleChange}
                            placeholder="e.g., octocat"
                            disabled={!user}
                            className="w-full px-4 py-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="repo" className="block text-sm font-medium text-gray-300 mb-2">
                            Repository Name
                        </label>
                        <input
                            type="text"
                            id="repo"
                            name="repo"
                            value={formData.repo}
                            onChange={handleChange}
                            placeholder="e.g., Hello-World"
                            disabled={!user}
                            className="w-full px-4 py-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            required
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label htmlFor="baseBranch" className="block text-sm font-medium text-gray-300 mb-2">
                                Base Branch
                            </label>
                            <input
                                type="text"
                                id="baseBranch"
                                name="baseBranch"
                                value={formData.baseBranch}
                                onChange={handleChange}
                                placeholder="e.g., main"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="headBranch" className="block text-sm font-medium text-gray-300 mb-2">
                                Head Branch
                            </label>
                            <input
                                type="text"
                                id="headBranch"
                                name="headBranch"
                                value={formData.headBranch}
                                onChange={handleChange}
                                placeholder="e.g., feature-branch"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400">
                            {error}
                        </div>
                    )}

                    {success && (
                        <div className="p-4 bg-green-500/10 border border-green-500/30 rounded-lg text-green-400">
                            {success}
                        </div>
                    )}

                    <Button
                        type="submit"
                        disabled={loading || !user}
                        className="w-full h-12 text-lg font-semibold"
                    >
                        {loading ? 'Comparing...' : 'Compare Branches'}
                    </Button>

                    {/* Generate PR Suggestion Button */}
                    {compareId && (
                        <Button
                            type="button"
                            onClick={handleGeneratePR}
                            disabled={generatingPR || !user}
                            className="w-full h-12 text-lg font-semibold bg-purple-600 hover:bg-purple-700"
                        >
                            {generatingPR ? 'Generating...' : '✨ Generate PR Title & Description'}
                        </Button>
                    )}

                    {/* PR Suggestion Display */}
                    {prSuggestion && (
                        <div className="mt-6 p-6 bg-gradient-to-br from-purple-900/30 to-blue-900/30 border border-purple-500/50 rounded-lg">
                            <h3 className="text-xl font-bold text-purple-300 mb-4 flex items-center gap-2">
                                <span>✨</span> AI-Generated PR Suggestion
                            </h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Title
                                    </label>
                                    <div className="p-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white">
                                        {prSuggestion.title}
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Description
                                    </label>
                                    <div className="p-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white whitespace-pre-wrap">
                                        {prSuggestion.description}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </form>
            </Card>
        </div>
    );
};

export default CompareForm;
