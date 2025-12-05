import React, { useEffect, useState, useRef } from 'react';
import axios from 'axios';
import { Button } from "@/components/ui/button";
import CompareForm from '@/components/CompareForm';

interface User {
    id: string;
    name: string;
    avatar_url: string;
    email: string;
}

const Home: React.FC = () => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const formRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        fetchUser();
    }, []);

    const fetchUser = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/user', {
                withCredentials: true,
            });
            setUser(response.data);
        } catch (error) {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    const handleGetStarted = () => {
        if (user) {
            // User is logged in, scroll to form
            formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            // User is not logged in, redirect to login
            window.location.href = 'http://localhost:8080/oauth2/authorization/github';
        }
    };

    return (
        <div className="flex min-h-screen flex-col bg-background">
            {/* Hero Section */}
            <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center py-12 md:py-24 lg:py-32">
                <div className="container px-4 md:px-6">
                    <div className="flex flex-col items-center space-y-4 text-center">
                        <div className="space-y-4">
                            <h1 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl lg:text-6xl/none pixel-gradient bg-clip-text text-transparent pb-2">
                                Streamline Your Pull Requests
                            </h1>
                            <p className="mx-auto max-w-[700px] text-gray-400 md:text-xl">
                                Experience a seamless workflow for creating and updating pull requests. Integrate Jira tickets or Github issues, and code change to generate detailed descriptions effortlessly.
                            </p>
                        </div>
                        <div className="space-x-4">
                            <Button
                                onClick={handleGetStarted}
                                size="lg"
                                className="h-12 px-8"
                                disabled={loading}
                            >
                                {loading ? 'Loading...' : 'Get Started'}
                            </Button>
                            <Button variant="outline" size="lg" className="h-12 px-8">
                                About Us
                            </Button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Compare Form Section */}
            <div ref={formRef} className="flex min-h-screen flex-col items-center justify-center py-12 md:py-24 lg:py-32 px-4">
                <CompareForm />
            </div>
        </div>
    );
};

export default Home;

