import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Github, Lock, Sparkles, Ticket } from "lucide-react";

interface CompareFormData {
owner: string;
repo: string;
baseBranch: string;
headBranch: string;
jiraTicketKey?: string;
}

interface User {
id: string;
name: string;
avatar_url: string;
email: string;
}

interface CompareResult {
id: number;
filesChanged: number;
additions: number;
deletions: number;
}

interface PRSuggestion {
title: string;
description: string;
}

const CompareForm: React.FC = () => {
const [formData, setFormData] = useState<CompareFormData>({
owner: '',
repo: '',
baseBranch: 'main',
headBranch: '',
jiraTicketKey: ''
});

const [loading, setLoading] = useState(false);
const [error, setError] = useState<string | null>(null);
const [result, setResult] = useState<CompareResult | null>(null);
const [user, setUser] = useState<User | null>(null);
const [authLoading, setAuthLoading] = useState(true);
const [prSuggestion, setPrSuggestion] = useState<PRSuggestion | null>(null);

// JIRA tickets state
const [jiraTickets, setJiraTickets] = useState<Map<string, string>>(new Map());
const [jiraLoading, setJiraLoading] = useState(false);
const [jiraError, setJiraError] = useState<string | null>(null);

useEffect(() => {
    checkAuthentication();
}, []);

// Fetch JIRA tickets when user is authenticated
useEffect(() => {
    if (user?.id) {
        fetchJiraTickets();
    }
}, [user]);

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

const fetchJiraTickets = async () => {
    console.log('Fetching JIRA tickets for user:', user);
    if (!user?.id) return;
    
    setJiraLoading(true);
    setJiraError(null);
    
    try {
        const response = await axios.get(
            `http://localhost:8080/jiraConnection/oauth/allTickets/${user.id}`,
            { withCredentials: true }
        );
        
        // Convert response object to Map<string, string> for easier handling
        const ticketsObj = response.data;
        const ticketsMap = new Map<string, string>(
            Object.entries(ticketsObj).map(([k, v]) => [k, String(v)])
        );
        setJiraTickets(ticketsMap);
    } catch (error) {
        console.error('Error fetching JIRA tickets:', error);
        setJiraError('Failed to load JIRA tickets');
    } finally {
        setJiraLoading(false);
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

const handleSelectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
        ...prev,
        [name]: value
    }));
};

const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    setPrSuggestion(null);

    try {
        // Step 1: Compare branches
        const compareResponse = await fetch('http://localhost:8080/api/compare', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify(formData),
        });

        const compareData = await compareResponse.json();

        if (compareResponse.ok && compareData.success) {
            setResult(compareData.data);
            const compareId = compareData.data.id;

            // Step 2: choose endpoint based on Jira ticket
            const hasTicket = !!formData.jiraTicketKey?.trim();
            const baseUrl = `http://localhost:8080/api/compare/${compareId}`;
            const url = hasTicket
                ? `${baseUrl}/generate-pr-suggestion-with-jira?ticketKey=${encodeURIComponent(formData.jiraTicketKey!)}`
                : `${baseUrl}/generate-pr-suggestion`;

            const prResponse = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            const prData = await prResponse.json();

            if (prResponse.ok && prData.success) {
                setPrSuggestion(prData.data);
            } else {
                setError(prData.error || 'Failed to generate PR suggestion');
            }
        } else {
            setError(compareData.error || 'Failed to compare branches');
        }
    } catch (err) {
        console.error('Error:', err);
        setError('Failed to connect to server');
    } finally {
        setLoading(false);
    }
};

if (authLoading) {
    return (
        <div className="w-full max-w-3xl mx-auto">
            <Card className="p-8 bg-gray-950/80 backdrop-blur-xl border-gray-800 shadow-2xl">
                <div className="flex items-center justify-center py-12">
                    <div className="animate-pulse text-gray-500">Loading...</div>
                </div>
            </Card>
        </div>
    );
}

return (
    <div className="w-full max-w-3xl mx-auto">
        <Card className="p-8 bg-gray-950/80 backdrop-blur-xl border-gray-800 shadow-2xl relative overflow-hidden">
            {/* Subtle gradient overlay */}
            <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 via-transparent to-purple-500/5 pointer-events-none" />

            {/* Login Required Overlay */}
            {!user && (
                <div className="absolute inset-0 bg-gray-950/98 backdrop-blur-sm z-10 flex items-center justify-center p-8">
                    <div className="text-center space-y-6 max-w-md relative">
                        <div className="flex justify-center">
                            <div className="p-4 bg-gray-800/50 rounded-full border border-gray-700">
                                <Lock className="w-12 h-12 text-gray-400" />
                            </div>
                        </div>
                        <div className="space-y-2">
                            <h3 className="text-2xl font-bold text-white">
                                Authentication Required
                            </h3>
                            <p className="text-gray-400 text-sm">
                                Sign in with GitHub to compare branches and generate AI-powered PR descriptions
                            </p>
                        </div>
                        <Button
                            onClick={handleLogin}
                            size="lg"
                            className="gap-2 h-12 px-8 bg-white text-black hover:bg-gray-200"
                        >
                            <Github className="w-5 h-5" />
                            Sign in with GitHub
                        </Button>
                    </div>
                </div>
            )}

            <div className="relative">
                <h2 className="text-3xl font-bold text-center mb-2 text-white">
                    Compare Branches
                </h2>
                <p className="text-gray-400 text-center mb-8 text-sm">
                    AI-powered branch comparison and PR generation
                </p>

                <form onSubmit={handleSubmit} className="space-y-5">
                    {/* JIRA Ticket Dropdown */}
                    <div>
                        <label htmlFor="jiraTicketKey" className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide flex items-center gap-2">
                            <Ticket className="w-4 h-4" />
                            JIRA Ticket (Optional)
                        </label>
                        <select
                            id="jiraTicketKey"
                            name="jiraTicketKey"
                            value={formData.jiraTicketKey || ''}
                            onChange={handleSelectChange}
                            disabled={!user || jiraLoading}
                            className="w-full px-4 py-3 bg-gray-900/50 border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <option value="">
                                {jiraLoading ? 'Loading tickets...' : 'Select a JIRA ticket'}
                            </option>
                            {Array.from(jiraTickets.entries()).map(([key, summary]) => (
                                <option key={key} value={key}>
                                    {key} - {summary}
                                </option>
                            ))}
                        </select>
                        {jiraError && (
                            <p className="mt-2 text-xs text-red-400 flex items-center gap-1">
                                <span>⚠</span> {jiraError}
                                <button
                                    type="button"
                                    onClick={fetchJiraTickets}
                                    className="ml-2 underline hover:text-red-300"
                                >
                                    Retry
                                </button>
                            </p>
                        )}
                        {formData.jiraTicketKey && (
                            <p className="mt-2 text-xs text-green-400">
                                ✓ Selected: {formData.jiraTicketKey}
                            </p>
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label htmlFor="owner" className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Owner
                            </label>
                            <input
                                type="text"
                                id="owner"
                                name="owner"
                                value={formData.owner}
                                onChange={handleChange}
                                placeholder="octocat"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-900/50 border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="repo" className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Repository
                            </label>
                            <input
                                type="text"
                                id="repo"
                                name="repo"
                                value={formData.repo}
                                onChange={handleChange}
                                placeholder="my-repo"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-900/50 border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label htmlFor="baseBranch" className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Base Branch
                            </label>
                            <input
                                type="text"
                                id="baseBranch"
                                name="baseBranch"
                                value={formData.baseBranch}
                                onChange={handleChange}
                                placeholder="main"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-900/50 border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="headBranch" className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Head Branch
                            </label>
                            <input
                                type="text"
                                id="headBranch"
                                name="headBranch"
                                value={formData.headBranch}
                                onChange={handleChange}
                                placeholder="feature-branch"
                                disabled={!user}
                                className="w-full px-4 py-3 bg-gray-900/50 border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                                required
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
                            {error}
                        </div>
                    )}

                    {result && (
                        <div className="p-4 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
                            ✓ Comparison completed: {result.filesChanged} files changed
                        </div>
                    )}

                    <Button
                        type="submit"
                        disabled={loading || !user}
                        className="w-full h-13 text-base font-semibold bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white border-0 shadow-lg shadow-blue-500/20 transition-all duration-200"
                    >
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Analyzing & Generating...
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <Sparkles className="w-5 h-5" />
                                Compare & Generate PR
                            </span>
                        )}
                    </Button>
                </form>

                {/* PR Suggestion Display */}
                {prSuggestion && (
                    <div className="mt-8 p-6 bg-gray-900/50 border border-gray-800 rounded-lg space-y-5">
                        <div className="flex items-center gap-2 text-blue-400 font-semibold">
                            <Sparkles className="w-5 h-5" />
                            <span>AI-Generated PR Suggestion</span>
                        </div>

                        <div>
                            <label className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Title
                            </label>
                            <div className="p-4 bg-gray-950/50 border border-gray-800 rounded-lg text-white font-medium">
                                {prSuggestion.title}
                            </div>
                        </div>

                        <div>
                            <label className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">
                                Description
                            </label>
                            <div className="p-4 bg-gray-950/50 border border-gray-800 rounded-lg text-gray-300 whitespace-pre-wrap text-sm leading-relaxed">
                                {prSuggestion.description.replace(/\\n/g, '\n')}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </Card>
    </div>
);
};

export default CompareForm;